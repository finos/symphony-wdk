package com.symphony.bdk.workflow.configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.engine.camunda.WorkflowDirectedGraphService;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

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

  private WorkflowDeployer workflowDeployer;
  private WorkflowBotConfiguration workflowBotConfiguration;

  private WorkflowDirectedGraphService directedGraphService;

  @BeforeEach
  void setUp() {
    engine = mock(WorkflowEngine.class);
    workflowBotConfiguration = mock(WorkflowBotConfiguration.class);
    directedGraphService = mock(WorkflowDirectedGraphService.class);
    workflowDeployer = new WorkflowDeployer(engine, directedGraphService);

    when(workflowBotConfiguration.getWorkflowsFolderPath()).thenReturn(workflowsFolder.getPath());
  }

  @Test
  void workflowAlreadyInFolder() throws IOException, InterruptedException {
    WorkflowFolderWatcher watcher = new WorkflowFolderWatcher(workflowDeployer, workflowBotConfiguration);

    copyWorkflow();
    final Thread watcherThread = startWatcherThread(watcher);

    verify(engine, timeout(5_000)).deploy(any(Workflow.class));

    watcher.stopMonitoring();
    watcherThread.join();
  }

  @Test
  void workflowAddedInFolder() throws IOException, InterruptedException {
    WorkflowFolderWatcher watcher = new WorkflowFolderWatcher(workflowDeployer, workflowBotConfiguration);

    final Thread watcherThread = startWatcherThread(watcher);
    Thread.sleep(1_000); // just a small wait to (try) to make sure the folder is watched before copying file
    copyWorkflow();

    verify(engine, timeout(10_000)).deploy(any(Workflow.class));

    watcher.stopMonitoring();
    watcherThread.join();
  }

  @Test
  void workflowRemovedFromFolder() throws IOException, InterruptedException {
    WorkflowFolderWatcher watcher = new WorkflowFolderWatcher(workflowDeployer, workflowBotConfiguration);

    copyWorkflow();
    final Thread watcherThread = startWatcherThread(watcher);
    Thread.sleep(1_000); // just a small wait to (try) to make sure the folder is watched before copying file
    verify(engine, timeout(5_000)).deploy(any(Workflow.class));

    FileUtils.forceDelete(new File(workflowsFolder, "workflow.swadl.yaml"));
    verify(engine, timeout(10_000)).undeployByWorkflowId(any());

    watcher.stopMonitoring();
    watcherThread.join();
  }

  @Test
  void workflowModifiedInFolder() throws IOException, InterruptedException {
    WorkflowFolderWatcher watcher = new WorkflowFolderWatcher(workflowDeployer, workflowBotConfiguration);

    copyWorkflow();
    final Thread watcherThread = startWatcherThread(watcher);
    Thread.sleep(1_000); // just a small wait to (try) to make sure the folder is watched before copying file
    verify(engine, timeout(5_000)).deploy(any(Workflow.class));

    copyWorkflow();
    Thread.sleep(1_000); // just a small wait to (try) to make sure the folder is watched before copying file
    verify(engine, timeout(10_000).times(2)).deploy(any(Workflow.class));

    watcher.stopMonitoring();
    watcherThread.join();
  }

  private Thread startWatcherThread(WorkflowFolderWatcher watcher) {
    Thread watcherThread = new Thread(() -> {
      try {
        watcher.monitorWorkflowsFolder();
      } catch (IOException ioException) {
        throw new RuntimeException(ioException);
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
