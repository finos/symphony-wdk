package com.symphony.bdk.workflow.configuration;

import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;
import com.symphony.bdk.workflow.versioning.service.VersioningService;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class WorkflowDeployer {

  private final WorkflowEngine<BpmnModelInstance> workflowEngine;
  private final VersioningService versioningService;
  private final Map<Path, Triple<String, String, Boolean>> deployedWorkflows = new HashMap<>();

  private final Map<Pair<String, String>, Path> workflowIdPathMap = new HashMap<>();

  public WorkflowDeployer(@Autowired WorkflowEngine<BpmnModelInstance> workflowEngine,
      VersioningService versioningService) {
    this.workflowEngine = workflowEngine;
    this.versioningService = versioningService;
  }

  public void addAllWorkflowsFromFolder(Path path) {
    if (!Files.isDirectory(path)) {
      throw new IllegalArgumentException("Could not find workflows folder to monitor with path: " + path);
    }

    log.info("Watching workflows from {}", path);
    File[] existingFiles = path.toFile().listFiles();
    if (existingFiles != null) {
      for (File file : existingFiles) {
        if (isYaml(file.toPath())) {
          try {
            this.addWorkflow(file.toPath());
          } catch (Exception e) {
            log.error("Failed to add workflow for file {}", file, e);
          }
        }
      }
    }
  }

  public void addWorkflow(Path workflowFile) throws IOException, ProcessingException {
    if (workflowFile.toFile().length() == 0) {
      return;
    }
    log.debug("Adding a new workflow");
    Workflow workflow = SwadlParser.fromYaml(workflowFile.toFile());
    BpmnModelInstance instance = workflowEngine.parseAndValidate(workflow);

    Triple<String, String, Boolean> deployedWorkflow = deployedWorkflows.get(workflowFile);
    if (workflow.isToPublish()) {
      log.debug("Deploying this new workflow");
      workflowEngine.deploy(workflow, instance);

      // persist swadl
      String swadl = Files.readString(workflowFile.toFile().toPath(), StandardCharsets.UTF_8);
      this.persistSwadl(workflow.getId(), workflow.getVersion(), swadl);

    } else if (deployedWorkflow != null && deployedWorkflow.getRight()) {
      log.debug("Workflow is a draft version, undeploy the old version");
      workflowEngine.undeploy(deployedWorkflow.getLeft());
    }
    deployedWorkflows.put(workflowFile, Triple.of(workflow.getId(), workflow.getVersion(), workflow.isToPublish()));
    workflowIdPathMap.put(Pair.of(workflow.getId(), workflow.getVersion()), workflowFile);
  }

  private void persistSwadl(String workflowId, String version, String swadl) {
    this.versioningService.save(workflowId, version, swadl);
  }


  public void handleFileEvent(Path changedFile, WatchEvent<Path> event) throws IOException, ProcessingException {
    if (isYaml(changedFile)) {
      if (event.kind().equals(StandardWatchEventKinds.ENTRY_CREATE)) {
        this.addWorkflow(changedFile);

      } else if (event.kind().equals(StandardWatchEventKinds.ENTRY_DELETE)) {
        String workflowId = deployedWorkflows.get(changedFile).getLeft();
        String workflowVersion = deployedWorkflows.get(changedFile).getMiddle();
        this.workflowEngine.undeploy(workflowId);
        this.deployedWorkflows.remove(changedFile);
        this.workflowIdPathMap.remove(Pair.of(workflowId, workflowVersion));

      } else if (event.kind().equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
        this.addWorkflow(changedFile);

      } else {
        log.debug("Unknown event: {}", event);
      }
    }
  }

  private boolean isYaml(Path changedFile) {
    return changedFile.toString().endsWith(".yaml") || changedFile.toString().endsWith(".yml");
  }

  public boolean workflowExist(String id, String version) {
    return workflowIdPathMap.containsKey(Pair.of(id, version));
  }

  public Path workflowSwadlPath(String id, String version) {
    return workflowIdPathMap.get(Pair.of(id, version));
  }

  public List<Path> workflowSwadlPath(String id) {
    return workflowIdPathMap.keySet()
        .stream()
        .filter(key -> key.getLeft().equals(id))
        .map(workflowIdPathMap::get)
        .collect(Collectors.toList());
  }

  public Set<Path> workflowSwadlPaths() {
    return deployedWorkflows.keySet();
  }
}
