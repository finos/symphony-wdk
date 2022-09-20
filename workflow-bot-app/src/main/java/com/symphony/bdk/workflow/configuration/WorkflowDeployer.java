package com.symphony.bdk.workflow.configuration;

import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class WorkflowDeployer {

  private final WorkflowEngine<?> workflowEngine;
  private final Map<Path, Pair<String, Boolean>> deployedWorkflows = new HashMap<>();

  public WorkflowDeployer(@Autowired WorkflowEngine<?> workflowEngine) {
    this.workflowEngine = workflowEngine;
  }

  public void addAllWorkflowsFromFolder(String workflowsFolder, Path path) {
    if (!Files.isDirectory(path)) {
      throw new IllegalArgumentException("Could not find workflows folder to monitor with path: " + workflowsFolder);
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
    Object instance = workflowEngine.parseAndValidate(workflow);
    Pair<String, Boolean> deployedWorkflow = deployedWorkflows.get(workflowFile);
    if (workflow.isToPublish()) {
      log.debug("Deploying this new workflow");
      workflowEngine.deploy(workflow, instance);
    } else if (deployedWorkflow != null && deployedWorkflow.getRight()) {
      log.debug("Workflow is a draft version, undeloying the old version");
      workflowEngine.undeploy(deployedWorkflow.getLeft());
    }
    deployedWorkflows.put(workflowFile, Pair.of(workflow.getId(), workflow.isToPublish()));
  }


  public void handleFileEvent(Path changedFile, WatchEvent<Path> event) throws IOException, ProcessingException {
    if (isYaml(changedFile)) {
      if (event.kind().equals(StandardWatchEventKinds.ENTRY_CREATE)) {
        this.addWorkflow(changedFile);

      } else if (event.kind().equals(StandardWatchEventKinds.ENTRY_DELETE)) {
        this.workflowEngine.undeploy(deployedWorkflows.get(changedFile).getLeft());
        this.deployedWorkflows.remove(changedFile);

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

}
