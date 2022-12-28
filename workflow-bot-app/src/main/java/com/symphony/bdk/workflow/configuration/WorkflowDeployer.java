package com.symphony.bdk.workflow.configuration;

import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.exception.NotFoundException;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;
import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;
import com.symphony.bdk.workflow.versioning.service.VersioningService;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class WorkflowDeployer {

  private final WorkflowEngine<BpmnModelInstance> workflowEngine;
  private final VersioningService versioningService;

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

    Optional<VersionedWorkflow> deployedWorkflow = this.versioningService.find(workflowFile);
    if (workflow.isToPublish()) {
      log.debug("Deploying this new workflow");
      workflowEngine.deploy(workflow, instance);

      // persist swadl
      String swadl = Files.readString(workflowFile.toFile().toPath(), StandardCharsets.UTF_8);
      this.persistWorkflow(workflow.getId(), workflow.getVersion(), swadl, workflowFile.toString());

    } else if (deployedWorkflow.isPresent() && deployedWorkflow.get().isToPublish()) {
      log.debug("Workflow is a draft version, undeploy the old version");
      workflowEngine.undeploy(deployedWorkflow.get().getVersionedWorkflowId().getId());
    }
  }

  private void persistWorkflow(String workflowId, String version, String swadl, String swadlPath) {
    this.versioningService.save(workflowId, version, swadl, swadlPath);
  }

  public void handleFileEvent(Path changedFile, WatchEvent<Path> event) throws IOException, ProcessingException {
    if (isYaml(changedFile)) {
      if (event.kind().equals(StandardWatchEventKinds.ENTRY_CREATE)) {
        this.addWorkflow(changedFile);

      } else if (event.kind().equals(StandardWatchEventKinds.ENTRY_DELETE)) {
        Optional<VersionedWorkflow> versionedWorkflow = this.versioningService.find(changedFile);
        if (versionedWorkflow.isPresent()) {
          String workflowId = versionedWorkflow.get().getVersionedWorkflowId().getId();
          String workflowVersion = versionedWorkflow.get().getVersionedWorkflowId().getVersion();
          this.workflowEngine.undeploy(workflowId);
          this.versioningService.delete(workflowId, workflowVersion);
        }

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
    return this.versioningService.find(id, version).isPresent();
  }

  public boolean workflowExist(String id) {
    return !this.versioningService.find(id).isEmpty();
  }

  public Path workflowSwadlPath(String id, String version) {
    return this.versioningService.find(id, version)
        .map(workflow -> Path.of(workflow.getPath()))
        .orElseThrow(
            () -> new NotFoundException(String.format("Version %s of the workflow %s does not exist", version, id)));
  }

  public List<Path> workflowSwadlPath(String id) {
    return this.versioningService.find(id)
        .stream()
        .map(workflow -> Path.of(workflow.getPath()))
        .collect(Collectors.toList());
  }

  public boolean isPathAlreadyExist(Path path) {
    return this.versioningService.find(path).isPresent();
  }
}
