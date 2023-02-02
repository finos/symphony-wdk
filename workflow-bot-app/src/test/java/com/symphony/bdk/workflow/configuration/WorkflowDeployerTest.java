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

import com.symphony.bdk.workflow.engine.WorkflowDirectedGraph;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.engine.camunda.CamundaTranslatedWorkflowContext;
import com.symphony.bdk.workflow.engine.camunda.WorkflowDirectedGraphService;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
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
  WorkflowEngine<CamundaTranslatedWorkflowContext> workflowEngine;
  @Mock
  WorkflowDirectedGraphService directedGraphService;

  @InjectMocks
  WorkflowDeployer workflowDeployer;

  @Test
  void testAddAllWorkflowsFromFolder() {
    CamundaTranslatedWorkflowContext context = mock(CamundaTranslatedWorkflowContext.class);
    String swadlFolderPath = "src/test/resources/basic/publish/";
    String deploymentId = "ABC";

    when(workflowEngine.translate(any(Workflow.class))).thenReturn(context);
    when(workflowEngine.deploy(any(CamundaTranslatedWorkflowContext.class))).thenReturn(deploymentId);

    workflowDeployer.addAllWorkflowsFromFolder(Path.of(swadlFolderPath));
    verify(workflowEngine).deploy(any(CamundaTranslatedWorkflowContext.class));
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
    String workflowFile = "src/test/resources/basic/publish/basic-workflow.swadl.yaml";
    String workflowId = "basic-workflow";
    String deploymentId = "ABC";

    CamundaTranslatedWorkflowContext context = mock(CamundaTranslatedWorkflowContext.class);
    when(workflowEngine.translate(any(Workflow.class))).thenReturn(context);
    when(workflowEngine.deploy(any(CamundaTranslatedWorkflowContext.class))).thenReturn(deploymentId);
    when(directedGraphService.putDirectedGraph(any())).thenReturn(mock(WorkflowDirectedGraph.class));

    workflowDeployer.handleFileEvent(Path.of(workflowFile), new WatchEvent(StandardWatchEventKinds.ENTRY_CREATE));

    verify(workflowEngine).deploy(eq(context));
    verify(workflowEngine, never()).undeployByWorkflowId(eq(workflowId));
  }

  @Test
  void testUpdateWorkflowDraft_workflowAlreadyExists() throws IOException, ProcessingException {
    String workflowFile = "src/test/resources/basic/draft/basic-draft-workflow.swadl.yaml";
    CamundaTranslatedWorkflowContext context = mock(CamundaTranslatedWorkflowContext.class);
    when(workflowEngine.translate(any(Workflow.class))).thenReturn(context);

    workflowDeployer.handleFileEvent(Path.of(workflowFile), new WatchEvent(StandardWatchEventKinds.ENTRY_MODIFY));
    verify(workflowEngine, never()).deploy(any(CamundaTranslatedWorkflowContext.class));
  }

  @Test
  void testHandleFileEventCreate() throws IOException, ProcessingException {
    final String workflowFile = "src/test/resources/basic/publish/basic-workflow.swadl.yaml";
    final String deploymentId = "ABC";

    CamundaTranslatedWorkflowContext context = mock(CamundaTranslatedWorkflowContext.class);
    when(workflowEngine.translate(any(Workflow.class))).thenReturn(context);
    when(workflowEngine.deploy(any(CamundaTranslatedWorkflowContext.class))).thenReturn(deploymentId);
    when(directedGraphService.putDirectedGraph(any())).thenReturn(mock(WorkflowDirectedGraph.class));

    workflowDeployer.handleFileEvent(Path.of(workflowFile), new WatchEvent(StandardWatchEventKinds.ENTRY_CREATE));
    verify(workflowEngine).deploy(any(CamundaTranslatedWorkflowContext.class));
  }

  @Test
  void testHandleFileEventModify() throws IOException, ProcessingException {
    final String workflowFile = "src/test/resources/basic/publish/basic-workflow.swadl.yaml";
    final String deploymentId = "ABC";
    CamundaTranslatedWorkflowContext context = mock(CamundaTranslatedWorkflowContext.class);
    when(workflowEngine.translate(any(Workflow.class))).thenReturn(context);
    when(workflowEngine.deploy(any(CamundaTranslatedWorkflowContext.class))).thenReturn(deploymentId);
    when(directedGraphService.putDirectedGraph(any())).thenReturn(mock(WorkflowDirectedGraph.class));

    workflowDeployer.handleFileEvent(Path.of(workflowFile), new WatchEvent(StandardWatchEventKinds.ENTRY_MODIFY));

    verify(workflowEngine).deploy(any(CamundaTranslatedWorkflowContext.class));
  }

  @Test
  void testHandleFileEventDeleteWorkflow() throws IOException, ProcessingException {
    String workflowFile = "src/test/resources/basic/publish/basic-workflow.swadl.yaml";
    Path path = Path.of(workflowFile);
    final String deploymentId = "ABC";
    CamundaTranslatedWorkflowContext context = mock(CamundaTranslatedWorkflowContext.class);
    when(workflowEngine.translate(any(Workflow.class))).thenReturn(context);
    when(workflowEngine.deploy(any(CamundaTranslatedWorkflowContext.class))).thenReturn(deploymentId);
    doNothing().when(workflowEngine).undeployByWorkflowId(eq("basic-workflow"));
    when(directedGraphService.putDirectedGraph(any())).thenReturn(mock(WorkflowDirectedGraph.class));
    workflowDeployer.addWorkflow(path);
    clearInvocations(workflowEngine);
    workflowDeployer.handleFileEvent(path, new WatchEvent(StandardWatchEventKinds.ENTRY_DELETE));

    verify(workflowEngine, never()).deploy(any(CamundaTranslatedWorkflowContext.class));
    verify(workflowEngine).undeployByWorkflowId(eq("basic-workflow"));
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
