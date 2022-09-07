package com.symphony.bdk.workflow.configuration;

import org.junit.jupiter.api.Disabled;

//TODO: fix unit test
@Disabled("Slow to run on Mac")
class WorkflowFolderWatcherTest {
/*
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

    verify(engine, timeout(5_000)).deploy(any());

    watcher.stopMonitoring();
    watcherThread.join();
  }

  @Test
  void workflowAddedInFolder() throws IOException, InterruptedException {
    WorkflowFolderWatcher watcher = new WorkflowFolderWatcher(workflowsFolder.getAbsolutePath(), engine);

    final Thread watcherThread = startWatcherThread(watcher);
    Thread.sleep(1_000); // just a small wait to (try) to make sure the folder is watched before copying file
    copyWorkflow();

    verify(engine, timeout(10_000)).deploy(any());

    watcher.stopMonitoring();
    watcherThread.join();
  }

  @Test
  void workflowRemovedFromFolder() throws IOException, InterruptedException {
    WorkflowFolderWatcher watcher = new WorkflowFolderWatcher(workflowsFolder.getAbsolutePath(), engine);

    copyWorkflow();
    final Thread watcherThread = startWatcherThread(watcher);
    Thread.sleep(1_000); // just a small wait to (try) to make sure the folder is watched before copying file
    verify(engine, timeout(5_000)).deploy(any());

    FileUtils.forceDelete(new File(workflowsFolder, "workflow.swadl.yaml"));
    verify(engine, timeout(10_000)).undeploy(any());

    watcher.stopMonitoring();
    watcherThread.join();
  }

  @Test
  void workflowModifiedInFolder() throws IOException, InterruptedException {
    WorkflowFolderWatcher watcher = new WorkflowFolderWatcher(workflowsFolder.getAbsolutePath(), engine);

    copyWorkflow();
    final Thread watcherThread = startWatcherThread(watcher);
    Thread.sleep(1_000); // just a small wait to (try) to make sure the folder is watched before copying file
    verify(engine, timeout(5_000)).deploy(any());

    copyWorkflow();
    verify(engine, timeout(10_000)).undeploy(any());
    verify(engine, timeout(10_000).times(2)).deploy(any());

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
  }*/
}
