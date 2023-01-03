package com.symphony.bdk.workflow.configuration;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowMgtAction;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.exception.DuplicateException;
import com.symphony.bdk.workflow.exception.NotFoundException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;
import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;
import com.symphony.bdk.workflow.versioning.service.VersioningService;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
public class WorkflowDeployerTest {

  @Mock
  WorkflowEngine workflowEngine;

  @Mock
  VersioningService versioningService;

  @InjectMocks
  WorkflowDeployer workflowDeployer;

  @Test
  void testWorkflowNotExists() {
    when(versioningService.findByWorkflowId("id")).thenReturn(Collections.emptyList());
    boolean exist = workflowDeployer.workflowExists("id");
    assertThat(exist).isFalse();
  }

  @Test
  void testWorkflowExists() {
    when(versioningService.findByWorkflowId("id")).thenReturn(Collections.singletonList(new VersionedWorkflow()));
    boolean exist = workflowDeployer.workflowExists("id");
    assertThat(exist).isTrue();
  }

  @Test
  void testWorkflowNotExistsWithIdAndVersion() {
    when(versioningService.findByWorkflowIdAndVersion("id", "v1")).thenReturn(Optional.empty());
    boolean exist = workflowDeployer.workflowExists("id", "v1");
    assertThat(exist).isFalse();
  }

  @Test
  void testWorkflowExistsWithIdAndVersion() {
    when(versioningService.findByWorkflowIdAndVersion("id", "v1")).thenReturn(Optional.of(new VersionedWorkflow()));
    boolean exist = workflowDeployer.workflowExists("id", "v1");
    assertThat(exist).isTrue();
  }

  @Test
  void testWorkflowSwadlPath() {
    final String path1 = "/path/to/swadl/id/v1";
    final String path2 = "/path/to/swadl/id/v2";
    when(versioningService.findByWorkflowId("id")).thenReturn(
        Arrays.asList(new VersionedWorkflow().setWorkflowId("id").setVersion("v0").setPath(path1),
            new VersionedWorkflow().setWorkflowId("id").setVersion("v1").setPath(path2)));
    List<Path> paths = workflowDeployer.workflowSwadlPath("id");
    assertThat(paths).hasSize(2);
    assertThat(paths).containsExactlyInAnyOrder(Path.of(path1), Path.of(path2));
  }

  @Test
  void testWorkflowSwadlPathEmpty() {
    when(versioningService.findByWorkflowIdAndVersion("id", "v1")).thenReturn(Optional.empty());

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> workflowDeployer.workflowSwadlPath("id", "v1"))
        .satisfies(e -> assertThat(e.getMessage()).isEqualTo("Version v1 of the workflow id does not exist"));
  }

  @Test
  void testWorkflowSwadlPathWithIdAndVersion() {
    when(versioningService.findByWorkflowIdAndVersion("id", "v1"))
        .thenReturn(Optional.of(new VersionedWorkflow().setPath("/path/to/swadl")));
    Path path = workflowDeployer.workflowSwadlPath("id", "v1");
    assertThat(path.toString()).isEqualTo("/path/to/swadl");
  }

  @Test
  void testWorkflowSwadlPathWithIdAndVersionEmpty() {
    when(versioningService.findByWorkflowIdAndVersion("id", "v1")).thenReturn(Optional.empty());

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> workflowDeployer.workflowSwadlPath("id", "v1"))
        .satisfies(e -> assertThat(e.getMessage()).isEqualTo("Version v1 of the workflow id does not exist"));
  }

  @Test
  void testIsPathAlreadyExist() {
    when(versioningService.findByPath(any(Path.class))).thenReturn(Optional.of(new VersionedWorkflow()));
    boolean pathAlreadyExist = workflowDeployer.isPathAlreadyExist(Path.of(""));
    assertThat(pathAlreadyExist).isTrue();
  }

  @Test
  void testIsPathAlreadyNotExist() {
    when(versioningService.findByPath(any(Path.class))).thenReturn(Optional.empty());
    boolean pathAlreadyExist = workflowDeployer.isPathAlreadyExist(Path.of(""));
    assertThat(pathAlreadyExist).isFalse();
  }

  @Test
  void testAddAllWorkflowsFromFolder() {
    BpmnModelInstance mockInstance = mock(BpmnModelInstance.class);
    String workflowId = "basic-workflow";
    String swadlFolderPath = "src/test/resources/basic/publish/";
    String swadlFilePath = swadlFolderPath + "basic-workflow.swadl.yaml";
    String deploymentId = "ABC";

    when(workflowEngine.parseAndValidate(any(Workflow.class))).thenReturn(mockInstance);
    when(workflowEngine.deploy(any(Workflow.class), eq(mockInstance))).thenReturn(deploymentId);
    when(versioningService.findByWorkflowIdAndVersion(eq(workflowId), eq(""))).thenReturn(Optional.empty());
    doNothing().when(versioningService).save(eq(workflowId), eq(""), any(), eq(swadlFilePath), eq(deploymentId));

    workflowDeployer.addAllWorkflowsFromFolder(Path.of(swadlFolderPath));

    verify(versioningService).save(eq(workflowId), eq(""), any(String.class), eq(swadlFilePath), eq(deploymentId));
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
  void testAddWorkflowDraft_workflowAlreadyExists() {
    String workflowFile = "src/test/resources/basic/draft/basic-draft-workflow.swadl.yaml";
    String workflowId = "basic-draft-workflow";

    when(versioningService.findByWorkflowIdAndVersion(eq(workflowId), eq("")))
        .thenReturn(Optional.of(new VersionedWorkflow()));

    assertThatExceptionOfType(DuplicateException.class).isThrownBy(() -> {
      workflowDeployer.addWorkflow(Path.of(workflowFile), WorkflowMgtAction.DEPLOY);
    }).satisfies(e -> assertThat(e.getMessage()).isEqualTo(
        String.format("Version %s of the workflow %s already exists", "", workflowId)));
  }

  @Test
  void testAddWorkflowDraft_workflowNotExists() throws IOException, ProcessingException {
    String workflowFile = "src/test/resources/basic/draft/basic-draft-workflow.swadl.yaml";
    String workflowId = "basic-draft-workflow";

    when(versioningService.findByWorkflowIdAndVersion(eq(workflowId), eq("")))
        .thenReturn(Optional.empty());

    workflowDeployer.addWorkflow(Path.of(workflowFile), WorkflowMgtAction.DEPLOY);

    verify(versioningService, never()).save(any(), any(), any(), any(), any());
    verify(versioningService, never()).delete(any());
    verify(workflowEngine, never()).undeploy(any());
    verify(workflowEngine, never()).deploy(any());
  }

  @Test
  void testAddWorkflowPublish_workflowAlreadyExists() {
    String workflowFile = "src/test/resources/basic/publish/basic-workflow.swadl.yaml";
    String workflowId = "basic-workflow";

    when(versioningService.findByWorkflowIdAndVersion(eq(workflowId), eq("")))
        .thenReturn(Optional.of(new VersionedWorkflow()));

    assertThatExceptionOfType(DuplicateException.class).isThrownBy(() -> {
      workflowDeployer.addWorkflow(Path.of(workflowFile), WorkflowMgtAction.DEPLOY);
    }).satisfies(e -> assertThat(e.getMessage()).isEqualTo(
        String.format("Version %s of the workflow %s already exists", "", workflowId)));
  }

  @Test
  void testAddWorkflowPublish_workflowNotExists() throws IOException, ProcessingException {
    BpmnModelInstance mockInstance = mock(BpmnModelInstance.class);
    String workflowFile = "src/test/resources/basic/publish/basic-workflow.swadl.yaml";
    String workflowId = "basic-workflow";
    String deploymentId = "ABC";

    when(workflowEngine.parseAndValidate(any(Workflow.class))).thenReturn(mockInstance);
    when(workflowEngine.deploy(any(Workflow.class), eq(mockInstance))).thenReturn(deploymentId);
    when(versioningService.findByWorkflowIdAndVersion(eq(workflowId), eq(""))).thenReturn(Optional.empty());
    doNothing().when(versioningService).save(eq(workflowId), eq(""), any(), eq(workflowFile), eq(deploymentId));

    workflowDeployer.addWorkflow(Path.of(workflowFile), WorkflowMgtAction.DEPLOY);

    verify(versioningService).save(eq(workflowId), eq(""), any(String.class), eq(workflowFile), eq(deploymentId));
    verify(workflowEngine).deploy(any(Workflow.class), eq(mockInstance));
  }

  @Test
  void testUpdateWorkflowDraft_workflowAlreadyExists() throws IOException, ProcessingException {
    String workflowFile = "src/test/resources/basic/draft/basic-draft-workflow.swadl.yaml";
    String workflowId = "basic-draft-workflow";
    VersionedWorkflow versionedWorkflow = new VersionedWorkflow()
        .setWorkflowId(workflowId)
        .setVersion("")
        .setIsToPublish(true);

    when(versioningService.findByWorkflowIdAndVersion(eq(workflowId), eq("")))
        .thenReturn(Optional.of(versionedWorkflow));
    doNothing().when(versioningService).delete(eq(workflowId));
    doNothing().when(workflowEngine).undeploy(eq(workflowId));

    workflowDeployer.addWorkflow(Path.of(workflowFile), WorkflowMgtAction.UPDATE);

    verify(versioningService).findByWorkflowIdAndVersion(eq(workflowId), eq(""));
    verify(versioningService).delete(eq(workflowId));
    verify(workflowEngine, never()).deploy(any(Workflow.class), any(BpmnModelInstance.class));
    verify(workflowEngine).undeploy(eq(workflowId));
  }

  @Test
  void testUpdateWorkflowDraft_workflowNotExists() {
    String workflowFile = "src/test/resources/basic/draft/basic-draft-workflow.swadl.yaml";
    String workflowId = "basic-draft-workflow";

    when(versioningService.findByWorkflowIdAndVersion(eq(workflowId), eq(""))).thenReturn(Optional.empty());

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      workflowDeployer.addWorkflow(Path.of(workflowFile), WorkflowMgtAction.UPDATE);
    }).satisfies(e -> assertThat(e.getMessage()).isEqualTo(
        String.format("Version %s of the workflow %s does not exist", "", workflowId)));

    verify(versioningService).findByWorkflowIdAndVersion(eq(workflowId), eq(""));
    verify(versioningService, never()).delete(any());
    verify(workflowEngine, never()).deploy(any(Workflow.class), any(BpmnModelInstance.class));
    verify(workflowEngine, never()).undeploy(any());
  }

  @Test
  void testUpdateWorkflowPublish_workflowAlreadyExists() throws IOException, ProcessingException {
    BpmnModelInstance mockInstance = mock(BpmnModelInstance.class);
    String workflowFile = "src/test/resources/basic/publish/basic-workflow.swadl.yaml";
    String workflowId = "basic-workflow";
    String deploymentId = "ABC";
    VersionedWorkflow versionedWorkflow = new VersionedWorkflow()
        .setWorkflowId(workflowId)
        .setVersion("")
        .setIsToPublish(true);

    when(versioningService.findByWorkflowIdAndVersion(eq(workflowId), eq("")))
        .thenReturn(Optional.of(versionedWorkflow));
    when(workflowEngine.parseAndValidate(any(Workflow.class))).thenReturn(mockInstance);
    when(workflowEngine.deploy(any(Workflow.class), eq(mockInstance))).thenReturn(deploymentId);


    workflowDeployer.addWorkflow(Path.of(workflowFile), WorkflowMgtAction.UPDATE);

    verify(versioningService).save(any(VersionedWorkflow.class), eq(deploymentId));
    verify(versioningService).findByWorkflowIdAndVersion(eq(workflowId), eq(""));
    verify(workflowEngine).deploy(any(Workflow.class), eq(mockInstance));
    verify(versioningService, never()).delete(any());
    verify(workflowEngine, never()).undeploy(any());
  }

  @Test
  void testUpdateWorkflowPublish_workflowNotExists() {
    String workflowFile = "src/test/resources/basic/publish/basic-workflow.swadl.yaml";
    String workflowId = "basic-workflow";

    when(versioningService.findByWorkflowIdAndVersion(eq(workflowId), eq(""))).thenReturn(Optional.empty());

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      workflowDeployer.addWorkflow(Path.of(workflowFile), WorkflowMgtAction.UPDATE);
    }).satisfies(e -> {
      assertThat(e.getMessage()).isEqualTo(
          String.format("Version %s of the workflow %s does not exist", "", workflowId));
    });
  }

  @Test
  void testHandleFileEventCreateDuplicate() {
    final String workflowFile = "src/test/resources/basic/publish/basic-workflow.swadl.yaml";
    final String workflowId = "basic-workflow";
    final VersionedWorkflow versionedWorkflow = new VersionedWorkflow().setWorkflowId(workflowId)
        .setVersion("")
        .setPath(workflowFile)
        .setIsToPublish(true);

    when(versioningService.findByWorkflowIdAndVersion(eq(workflowId), eq(""))).thenReturn(
        Optional.of(versionedWorkflow));

    assertThatExceptionOfType(DuplicateException.class)
        .isThrownBy(() -> workflowDeployer.handleFileEvent(Path.of(workflowFile),
            new WatchEvent(StandardWatchEventKinds.ENTRY_CREATE)))
        .satisfies(e -> assertThat(e.getMessage()).isEqualTo(
            String.format("Version %s of the %s already exists", "", "workflow basic-workflow")));
  }

  @Test
  void testHandleFileEventCreate() throws IOException, ProcessingException {
    final String workflowFile = "src/test/resources/basic/publish/basic-workflow.swadl.yaml";
    final String workflowId = "basic-workflow";

    when(versioningService.findByWorkflowIdAndVersion(eq(workflowId), eq(""))).thenReturn(Optional.empty());

    workflowDeployer.handleFileEvent(Path.of(workflowFile), new WatchEvent(StandardWatchEventKinds.ENTRY_CREATE));

  }

  @Test
  void testHandleFileEventModify() throws IOException, ProcessingException {
    final BpmnModelInstance mockInstance = mock(BpmnModelInstance.class);
    final String workflowFile = "src/test/resources/basic/publish/basic-workflow.swadl.yaml";
    final String workflowId = "basic-workflow";
    final String deploymentId = "ABC";
    final VersionedWorkflow versionedWorkflow = new VersionedWorkflow().setWorkflowId(workflowId)
        .setVersion("")
        .setPath(workflowFile)
        .setIsToPublish(true);

    when(versioningService.findByWorkflowIdAndVersion(eq(workflowId), eq(""))).thenReturn(
        Optional.of(versionedWorkflow));
    when(workflowEngine.parseAndValidate(any(Workflow.class))).thenReturn(mockInstance);
    when(workflowEngine.deploy(any(Workflow.class), eq(mockInstance))).thenReturn(deploymentId);

    workflowDeployer.handleFileEvent(Path.of(workflowFile), new WatchEvent(StandardWatchEventKinds.ENTRY_MODIFY));

    verify(versioningService).findByWorkflowIdAndVersion(eq(workflowId), eq(""));
    verify(workflowEngine).deploy(any(Workflow.class), eq(mockInstance));
    verify(versioningService).save(any(VersionedWorkflow.class), eq(deploymentId));
  }

  @Test
  void testHandleFileEventModifyNotFound() {
    final String workflowFile = "src/test/resources/basic/publish/basic-workflow.swadl.yaml";
    final String workflowId = "basic-workflow";

    when(versioningService.findByWorkflowIdAndVersion(eq(workflowId), eq(""))).thenReturn(Optional.empty());

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      workflowDeployer.handleFileEvent(Path.of(workflowFile), new WatchEvent(StandardWatchEventKinds.ENTRY_MODIFY));
    }).satisfies(e -> {
      assertThat(e.getMessage()).isEqualTo(
          String.format("Version %s of the workflow %s does not exist", "", workflowId));
    });
  }

  @Test
  void testHandleFileEventDeleteWorkflowFound() throws IOException, ProcessingException {
    String workflowFile = "src/test/resources/basic/publish/basic-workflow.swadl.yaml";
    Path path = Path.of(workflowFile);
    VersionedWorkflow versionedWorkflow = new VersionedWorkflow().setWorkflowId("basic-workflow")
        .setVersion("")
        .setPath(workflowFile)
        .setIsToPublish(true);

    when(versioningService.findByPath(eq(path))).thenReturn(Optional.of(versionedWorkflow));
    doNothing().when(versioningService).delete(eq("basic-workflow"));
    doNothing().when(workflowEngine).undeploy(eq("basic-workflow"));

    workflowDeployer.handleFileEvent(path, new WatchEvent(StandardWatchEventKinds.ENTRY_DELETE));

    verify(versioningService).delete(eq("basic-workflow"));
    verify(workflowEngine, never()).deploy(any(), any());
    verify(workflowEngine).undeploy(eq("basic-workflow"));
  }

  @Test
  void testHandleFileEventDeleteWorkflowNotFound() throws IOException, ProcessingException {
    String workflowFile = "src/test/resources/basic/publish/basic-workflow.swadl.yaml";
    Path path = Path.of(workflowFile);

    when(versioningService.findByPath(eq(path))).thenReturn(Optional.empty());

    workflowDeployer.handleFileEvent(path, new WatchEvent(StandardWatchEventKinds.ENTRY_DELETE));

    verify(versioningService, never()).delete(any());
    verify(workflowEngine, never()).deploy(any(), any());
    verify(workflowEngine, never()).undeploy(any());
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
