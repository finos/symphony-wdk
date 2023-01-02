package com.symphony.bdk.workflow.configuration;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowMgtAction;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.exception.DuplicateException;
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

  private static final String WORKFLOW_NOT_EXIST_EXCEPTION_MSG = "Version %s of the workflow %s does not exist";
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
            this.addWorkflow(file.toPath(), WorkflowMgtAction.DEPLOY);
          } catch (Exception e) {
            log.error("Failed to add workflow for file {}", file, e);
          }
        }
      }
    }
  }

  public void handleFileEvent(Path changedFile, WatchEvent<Path> event) throws IOException, ProcessingException {
    if (isYaml(changedFile)) {
      if (event.kind().equals(StandardWatchEventKinds.ENTRY_CREATE)) {
        this.addWorkflow(changedFile, WorkflowMgtAction.DEPLOY);

      } else if (event.kind().equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
        this.addWorkflow(changedFile, WorkflowMgtAction.UPDATE);

      } else if (event.kind().equals(StandardWatchEventKinds.ENTRY_DELETE)) {

        /*
          The workflow id is required for the version undeployment and deletion. Querying the database by the SWADL
          file path is expensive as the column is not indexed (and should not be indexed).
          But this is the only way to have the workflow id as it cannot be provided in parameters because the actual
          file is already deleted from the filesystem.
         */
        Optional<VersionedWorkflow> versionedWorkflow = this.versioningService.findByPath(changedFile);
        if (versionedWorkflow.isPresent()) {
          String workflowId = versionedWorkflow.get().getWorkflowId();
          this.workflowEngine.undeploy(workflowId);
          this.versioningService.delete(workflowId);
        }

      } else {
        log.debug("Unknown event: {}", event);
      }
    }
  }

  public void addWorkflow(Path workflowFile, WorkflowMgtAction action) throws IOException, ProcessingException {
    if (workflowFile.toFile().length() == 0) {
      return;
    }

    log.debug("Adding a new workflow");
    Workflow workflow = SwadlParser.fromYaml(workflowFile.toFile());
    Optional<VersionedWorkflow> deployedWorkflow =
        this.versioningService.findByWorkflowIdAndVersion(workflow.getId(), workflow.getVersion());

    if (action.equals(WorkflowMgtAction.UPDATE) && deployedWorkflow.isEmpty()) {
      throw new NotFoundException(
          String.format(WORKFLOW_NOT_EXIST_EXCEPTION_MSG, workflow.getVersion(), workflow.getId()));

    } else if (action.equals(WorkflowMgtAction.DEPLOY) && deployedWorkflow.isPresent()) {
      throw new DuplicateException(
          String.format("Version %s of the workflow %s already exists", workflow.getVersion(), workflow.getId()));
    }

    if (workflow.isToPublish()) {
      log.debug("Deploying this new workflow");
      BpmnModelInstance instance = workflowEngine.parseAndValidate(workflow);
      String deploymentId = workflowEngine.deploy(workflow, instance);
      String swadl = Files.readString(workflowFile.toFile().toPath(), StandardCharsets.UTF_8);

      // persist or update new SWADL
      switch (action) {
        case DEPLOY:
          this.persistNewWorkflow(workflow.getId(), workflow.getVersion(), deploymentId, swadl,
              workflowFile.toString());
          break;
        case UPDATE:
          this.updateWorkflow(deployedWorkflow.get(), deploymentId, swadl, workflowFile.toString());
          break;
        default:
          break;
      }

    } else if (deployedWorkflow.isPresent() && deployedWorkflow.get().isToPublish()) {
      log.debug("Workflow is a draft version, undeploy all old versions");
      workflowEngine.undeploy(deployedWorkflow.get().getWorkflowId());
      this.versioningService.delete(deployedWorkflow.get().getWorkflowId());
    }
  }

  private void persistNewWorkflow(String workflowId, String version, String deploymentId,
      String swadl, String swadlPath) {
    this.versioningService.save(workflowId, version, swadl, swadlPath, deploymentId);
  }

  private void updateWorkflow(VersionedWorkflow workflow, String deploymentId, String swadl, String path) {
    workflow.setSwadl(swadl);
    workflow.setPath(path);
    workflow.setDeploymentId(deploymentId);
    this.versioningService.save(workflow, deploymentId);
  }

  private boolean isYaml(Path changedFile) {
    return changedFile.toString().endsWith(".yaml") || changedFile.toString().endsWith(".yml");
  }

  public boolean workflowExists(String id, String version) {
    return this.versioningService.findByWorkflowIdAndVersion(id, version).isPresent();
  }

  public boolean workflowExists(String id) {
    return !this.versioningService.findByWorkflowId(id).isEmpty();
  }

  public Path workflowSwadlPath(String id, String version) {
    return this.versioningService.findByWorkflowIdAndVersion(id, version)
        .map(workflow -> Path.of(workflow.getPath()))
        .orElseThrow(
            () -> new NotFoundException(String.format(WORKFLOW_NOT_EXIST_EXCEPTION_MSG, version, id)));
  }

  public List<Path> workflowSwadlPath(String id) {
    return this.versioningService.findByWorkflowId(id)
        .stream()
        .map(workflow -> Path.of(workflow.getPath()))
        .collect(Collectors.toList());
  }

  public boolean isPathAlreadyExist(Path path) {
    return this.versioningService.findByPath(path).isPresent();
  }
}
