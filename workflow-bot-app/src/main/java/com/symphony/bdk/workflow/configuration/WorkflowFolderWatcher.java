package com.symphony.bdk.workflow.configuration;

import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

/**
 * Watch a specific folder for workflows.
 * Will automatically add workflows present at startup and update workflows on the fly while running
 * (stopping and redeploying them).
 */
@Generated // slow tests on Mac
@Slf4j
@Service
@ConditionalOnPropertyNotEmpty("wdk.workflows.path")
public class WorkflowFolderWatcher {

  private final Path workflowsFolder;
  private final WorkflowDeployer workflowDeployer;

  private WatchService watchService;

  public WorkflowFolderWatcher(@Autowired WorkflowDeployer workflowDeployer,
      @Autowired WorkflowBotConfiguration workflowBotConfiguration) {
    this.workflowDeployer = workflowDeployer;
    this.workflowsFolder = Paths.get(workflowBotConfiguration.getWorkflowsFolderPath());
  }

  @Scheduled(fixedDelay = Long.MAX_VALUE) // will run once after startup and wait for file events
  public void monitorWorkflowsFolder() throws IOException {
    this.watchService = FileSystems.getDefault().newWatchService();
    this.workflowDeployer.addAllWorkflowsFromFolder(this.workflowsFolder);

    workflowsFolder.register(this.watchService,
        StandardWatchEventKinds.ENTRY_DELETE,
        StandardWatchEventKinds.ENTRY_MODIFY,
        StandardWatchEventKinds.ENTRY_CREATE);
    watchFileEvents(workflowsFolder);
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
      @SuppressWarnings("unchecked")
      WatchEvent<Path> ev = (WatchEvent<Path>) event;
      Path changedFile = path.resolve(ev.context());
      this.workflowDeployer.handleFileEvent(changedFile, ev);
    } catch (Exception e) {
      log.error("Failed to update workflow for file change event {}", event.context(), e);
    }
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
