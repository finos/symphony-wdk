package com.symphony.bdk.workflow.configuration;

import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PreDestroy;

/**
 * Watch a specific folder for workflows.
 * Will automatically add workflows present at startup and update workflows on the fly while running
 * (stopping and redeploying them).
 */
@Generated // slow tests on Mac
@Slf4j
@Service
@ConditionalOnProperty(value = "workflows.folder")
public class WorkflowFolderWatcher {

  private final String workflowsFolder;
  private final WorkflowEngine<?> workflowEngine;
  private final Map<Path, Pair<String, Boolean>> deployedWorkflows = new HashMap<>();

  private WatchService watchService;

  public WorkflowFolderWatcher(@Value("${workflows.folder}") String workflowsFolder,
      @Autowired WorkflowEngine<?> workflowEngine) {
    this.workflowsFolder = workflowsFolder;
    this.workflowEngine = workflowEngine;
  }

  @Scheduled(fixedDelay = Long.MAX_VALUE) // will run once after startup and wait for file events
  public void monitorWorkflowsFolder() throws InterruptedException, IOException, ProcessingException {
    watchService = FileSystems.getDefault().newWatchService();
    Path path = Paths.get(workflowsFolder);

    if (!Files.isDirectory(path)) {
      throw new IllegalArgumentException("Could not find workflows folder to monitor with path: " + workflowsFolder);
    }

    log.info("Watching workflows from {}", path);
    File[] existingFiles = path.toFile().listFiles();
    if (existingFiles != null) {
      for (File file : existingFiles) {
        if (isYaml(file.toPath())) {
          try {
            addWorkflow(file.toPath());
          } catch (Exception e) {
            log.error("Failed to add workflow for file {}", file, e);
          }
        }
      }
    }

    path.register(watchService,
        StandardWatchEventKinds.ENTRY_DELETE,
        StandardWatchEventKinds.ENTRY_MODIFY,
        StandardWatchEventKinds.ENTRY_CREATE);
    watchFileEvents(path);
  }

  private void watchFileEvents(Path path) {
    try {
      WatchKey key;
      while ((key = watchService.take()) != null) {
        for (WatchEvent<?> event : key.pollEvents()) {
          handleFileEventOrLogError(path, event);
        }
        key.reset();
      }
    } catch (InterruptedException e) {
      // ignored, thrown when stopping watcher
      Thread.currentThread().interrupt();
    } catch (ClosedWatchServiceException e) {
      // ignored, thrown when stopping watcher
    }
  }

  private void handleFileEventOrLogError(Path path, WatchEvent<?> event) {
    try {
      handleFileEvent(path, event);
    } catch (Exception e) {
      log.error("Failed to update workflow for file change event {}", event.context(), e);
    }
  }

  @SuppressWarnings("unchecked")
  private void handleFileEvent(Path path, WatchEvent<?> event) throws IOException, ProcessingException {
    WatchEvent<Path> ev = (WatchEvent<Path>) event;
    Path changedFile = path.resolve(ev.context());

    if (isYaml(changedFile)) {

      if (ev.kind().equals(StandardWatchEventKinds.ENTRY_CREATE)) {
        addWorkflow(changedFile);

      } else if (ev.kind().equals(StandardWatchEventKinds.ENTRY_DELETE)) {
        workflowEngine.undeploy(deployedWorkflows.get(changedFile).getLeft());
        this.deployedWorkflows.remove(changedFile);

      } else if (ev.kind().equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
        addWorkflow(changedFile);

      } else {
        log.debug("Unknown event: {}", ev);
      }
    }
  }

  private boolean isYaml(Path changedFile) {
    return changedFile.toString().endsWith(".yaml") || changedFile.toString().endsWith(".yml");
  }

  private void addWorkflow(Path workflowFile) throws IOException, ProcessingException {
    if (workflowFile.toFile().length() == 0) {
      return;
    }
    Workflow workflow = SwadlParser.fromYaml(workflowFile.toFile());
    Object instance = workflowEngine.parseAndValidate(workflow);
    if (workflow.isToPublish()) {
      workflowEngine.deploy(workflow, instance);
    } else if (deployedWorkflows.get(workflowFile) != null && deployedWorkflows.get(workflowFile).getRight()) {
      workflowEngine.undeploy(deployedWorkflows.get(workflowFile).getLeft());
    }
    deployedWorkflows.put(workflowFile, Pair.of(workflow.getId(), workflow.isToPublish()));
  }

  @PreDestroy
  public void stopMonitoring() {
    try {
      watchService.close();
    } catch (IOException e) {
      log.error("Failed to stop monitoring workflows folder", e);
    }
  }
}
