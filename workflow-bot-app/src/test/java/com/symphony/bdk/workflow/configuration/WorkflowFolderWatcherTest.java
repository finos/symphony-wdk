package com.symphony.bdk.workflow.configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.symphony.bdk.workflow.engine.WorkflowEngine;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Disabled("Slow to run on Mac")
class WorkflowFolderWatcherTest {

  @TempDir
  File workflowsFolder;
  private WorkflowEngine engine;

  @BeforeEach
  void setUp() {
    engine = mock(WorkflowEngine.class);
  }

  @Test
  void workflowAlreadyInFolder() throws IOException, InterruptedException {
    WorkflowFolderWatcher watcher = new WorkflowFolderWatcher(workflowsFolder.getAbsolutePath(), engine);

    copyWorkflow();
    final Thread watcherThread = startWatcherThread(watcher);

    verify(engine, timeout(5_000)).execute(any());

    watcher.stopMonitoring();
    watcherThread.join();
  }

  @Test
  void workflowAddedInFolder() throws IOException, InterruptedException {
    WorkflowFolderWatcher watcher = new WorkflowFolderWatcher(workflowsFolder.getAbsolutePath(), engine);

    final Thread watcherThread = startWatcherThread(watcher);
    Thread.sleep(1_000); // just a small wait to (try) to make sure the folder is watched before copying file
    copyWorkflow();

    verify(engine, timeout(10_000)).execute(any());

    watcher.stopMonitoring();
    watcherThread.join();
  }

  @Test
  void workflowRemovedFromFolder() throws IOException, InterruptedException {
    WorkflowFolderWatcher watcher = new WorkflowFolderWatcher(workflowsFolder.getAbsolutePath(), engine);

    copyWorkflow();
    final Thread watcherThread = startWatcherThread(watcher);
    Thread.sleep(1_000); // just a small wait to (try) to make sure the folder is watched before copying file
    verify(engine, timeout(5_000)).execute(any());

    FileUtils.forceDelete(new File(workflowsFolder, "workflow.swadl.yaml"));
    verify(engine, timeout(10_000)).stop(any());

    watcher.stopMonitoring();
    watcherThread.join();
  }

  @Test
  void workflowModifiedInFolder() throws IOException, InterruptedException {
    WorkflowFolderWatcher watcher = new WorkflowFolderWatcher(workflowsFolder.getAbsolutePath(), engine);

    copyWorkflow();
    final Thread watcherThread = startWatcherThread(watcher);
    Thread.sleep(1_000); // just a small wait to (try) to make sure the folder is watched before copying file
    verify(engine, timeout(5_000)).execute(any());

    copyWorkflow();
    verify(engine, timeout(10_000)).stop(any());
    verify(engine, timeout(10_000).times(2)).execute(any());

    watcher.stopMonitoring();
    watcherThread.join();
  }

  private Thread startWatcherThread(WorkflowFolderWatcher watcher) {
    Thread watcherThread = new Thread(() -> {
      try {
        watcher.monitorWorkflowsFolder();
      } catch (InterruptedException | IOException | ProcessingException e) {
        throw new RuntimeException(e);
      }
    });
    watcherThread.start();
    return watcherThread;
  }

  private void copyWorkflow() throws IOException {
    FileOutputStream destination = new FileOutputStream(new File(workflowsFolder, "workflow.swadl.yaml"));
    IOUtils.copy(getClass().getResourceAsStream("workflow.swadl.yaml"), destination);
  }
}
