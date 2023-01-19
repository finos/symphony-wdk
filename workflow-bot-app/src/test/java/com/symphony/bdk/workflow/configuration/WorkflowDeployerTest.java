package com.symphony.bdk.workflow.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
public class WorkflowDeployerTest {

  @Mock
  WorkflowEngine workflowEngine;

  @InjectMocks
  WorkflowDeployer workflowDeployer;

  @Test
  void testAddAllWorkflowsFromFolder() {
    BpmnModelInstance mockInstance = mock(BpmnModelInstance.class);
    String swadlFolderPath = "src/test/resources/basic/publish/";
    String deploymentId = "ABC";

    when(workflowEngine.parseAndValidate(any(Workflow.class))).thenReturn(mockInstance);
    when(workflowEngine.deploy(any(Workflow.class), eq(mockInstance))).thenReturn(deploymentId);

    workflowDeployer.addAllWorkflowsFromFolder(Path.of(swadlFolderPath));
    verify(workflowEngine).deploy(any(Workflow.class), eq(mockInstance));
  }

  @Test
  void testAddAllWorkflowsFromFolderException() {
    String file = "src/test/resources/basic/publish/basic-workflow.swadl.yaml";
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> workflowDeployer.addAllWorkflowsFromFolder(Path.of(file)))
        .satisfies(
            e -> assertThat(e.getMessage()).isEqualTo("Could not find workflows folder to monitor with path: " + file));
  }

  @Test
  void testAddWorkflowPublish_workflowNotExists() throws IOException, ProcessingException {
    BpmnModelInstance mockInstance = mock(BpmnModelInstance.class);
    String workflowFile = "src/test/resources/basic/publish/basic-workflow.swadl.yaml";
    String workflowId = "basic-workflow";
    String deploymentId = "ABC";

    when(workflowEngine.parseAndValidate(any(Workflow.class))).thenReturn(mockInstance);
    when(workflowEngine.deploy(any(Workflow.class), eq(mockInstance))).thenReturn(deploymentId);

    workflowDeployer.handleFileEvent(Path.of(workflowFile), new WatchEvent(StandardWatchEventKinds.ENTRY_CREATE));

    verify(workflowEngine).deploy(any(Workflow.class), eq(mockInstance));
    verify(workflowEngine, never()).undeploy(eq(workflowId));
  }

  @Test
  void testUpdateWorkflowDraft_workflowAlreadyExists() throws IOException, ProcessingException {
    String workflowFile = "src/test/resources/basic/draft/basic-draft-workflow.swadl.yaml";
    String workflowId = "basic-draft-workflow";
    BpmnModelInstance mockInstance = mock(BpmnModelInstance.class);
    when(workflowEngine.parseAndValidate(any(Workflow.class))).thenReturn(mockInstance);

    workflowDeployer.handleFileEvent(Path.of(workflowFile), new WatchEvent(StandardWatchEventKinds.ENTRY_MODIFY));
    verify(workflowEngine, never()).deploy(any(Workflow.class), any(BpmnModelInstance.class));
  }

  @Test
  void testHandleFileEventCreate() throws IOException, ProcessingException {
    final String workflowFile = "src/test/resources/basic/publish/basic-workflow.swadl.yaml";
    final BpmnModelInstance mockInstance = mock(BpmnModelInstance.class);
    final String deploymentId = "ABC";

    when(workflowEngine.parseAndValidate(any(Workflow.class))).thenReturn(mockInstance);
    when(workflowEngine.deploy(any(Workflow.class), eq(mockInstance))).thenReturn(deploymentId);

    workflowDeployer.handleFileEvent(Path.of(workflowFile), new WatchEvent(StandardWatchEventKinds.ENTRY_CREATE));
    verify(workflowEngine).deploy(any(Workflow.class), eq(mockInstance));
  }

  @Test
  void testHandleFileEventModify() throws IOException, ProcessingException {
    final BpmnModelInstance mockInstance = mock(BpmnModelInstance.class);
    final String workflowFile = "src/test/resources/basic/publish/basic-workflow.swadl.yaml";
    final String deploymentId = "ABC";
    when(workflowEngine.parseAndValidate(any(Workflow.class))).thenReturn(mockInstance);
    when(workflowEngine.deploy(any(Workflow.class), eq(mockInstance))).thenReturn(deploymentId);

    workflowDeployer.handleFileEvent(Path.of(workflowFile), new WatchEvent(StandardWatchEventKinds.ENTRY_MODIFY));

    verify(workflowEngine).deploy(any(Workflow.class), eq(mockInstance));
  }

  @Test
  void testHandleFileEventDeleteWorkflow() throws IOException, ProcessingException {
    String workflowFile = "src/test/resources/basic/publish/basic-workflow.swadl.yaml";
    Path path = Path.of(workflowFile);
    final BpmnModelInstance mockInstance = mock(BpmnModelInstance.class);
    final String deploymentId = "ABC";
    when(workflowEngine.parseAndValidate(any(Workflow.class))).thenReturn(mockInstance);
    when(workflowEngine.deploy(any(Workflow.class), eq(mockInstance))).thenReturn(deploymentId);
    doNothing().when(workflowEngine).undeploy(eq("basic-workflow"));
    workflowDeployer.addWorkflow(path);
    clearInvocations(workflowEngine);
    workflowDeployer.handleFileEvent(path, new WatchEvent(StandardWatchEventKinds.ENTRY_DELETE));

    verify(workflowEngine, never()).deploy(any(), any());
    verify(workflowEngine).undeploy(eq("basic-workflow"));
  }

  private static class WatchEvent implements java.nio.file.WatchEvent<Path> {

    private final java.nio.file.WatchEvent.Kind<Path> kind;

    public WatchEvent(WatchEvent.Kind<Path> kind) {
      this.kind = kind;
    }

    @Override
    public Kind<Path> kind() {
      return this.kind;
    }

    @Override
    public int count() {
      return 0;
    }

    @Override
    public Path context() {
      return null;
    }
  }
}
