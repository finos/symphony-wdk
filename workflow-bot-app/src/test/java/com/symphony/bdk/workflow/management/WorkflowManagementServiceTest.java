package com.symphony.bdk.workflow.management;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.exception.NotFoundException;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Properties;
import com.symphony.bdk.workflow.swadl.v1.Workflow;
import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;
import com.symphony.bdk.workflow.versioning.repository.VersionedWorkflowRepository;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class WorkflowManagementServiceTest {

  @Mock
  VersionedWorkflowRepository versioningService;

  @Mock
  ObjectConverter conveter;

  @Mock
  WorkflowEngine<BpmnModelInstance> camundaEngine;

  @InjectMocks
  WorkflowManagementService workflowManagementService;


  static final String swadl = "id: test\n"
      + "activities:\n"
      + "  - send-message:\n"
      + "      id: msg\n"
      + "      on:\n"
      + "        message-received:\n"
      + "          content: msg\n"
      + "      content: content";

  Workflow workflow;

  public WorkflowManagementServiceTest() throws IOException, ProcessingException {
    workflow = SwadlParser.fromYaml(swadl);
  }

  @Test
  void testDeploy_existingActiveVersion_updateOldInsertNew() {
    when(conveter.convert(anyString(), eq(Workflow.class))).thenReturn(workflow);
    BpmnModelInstance instance = mock(BpmnModelInstance.class);
    when(camundaEngine.parseAndValidate(any(Workflow.class))).thenReturn(instance);
    when(camundaEngine.deploy(any(Workflow.class), any(BpmnModelInstance.class))).thenReturn("id");
    VersionedWorkflow activeVersion = new VersionedWorkflow();
    when(versioningService.save(any())).thenReturn(activeVersion);
    when(versioningService.saveAndFlush(any())).thenReturn(activeVersion);
    when(versioningService.findByWorkflowIdAndActiveTrue(eq("test"))).thenReturn(Optional.of(activeVersion));

    workflowManagementService.deploy(swadl);

    assertThat(activeVersion.getActive()).isFalse();
    verify(camundaEngine).deploy(any(Workflow.class), any(BpmnModelInstance.class));
    verify(versioningService).save(any());
    verify(versioningService).saveAndFlush(any());
  }

  @Test
  void testDeploy_noActiveVersion_insertNew() {
    when(conveter.convert(anyString(), eq(Workflow.class))).thenReturn(workflow);
    BpmnModelInstance instance = mock(BpmnModelInstance.class);
    when(camundaEngine.parseAndValidate(any(Workflow.class))).thenReturn(instance);
    when(camundaEngine.deploy(any(Workflow.class), any(BpmnModelInstance.class))).thenReturn("id");
    when(versioningService.save(any(VersionedWorkflow.class))).thenReturn(null);
    when(versioningService.findByWorkflowIdAndActiveTrue(eq("test"))).thenReturn(Optional.empty());

    workflowManagementService.deploy(swadl);

    verify(camundaEngine).deploy(any(Workflow.class), any(BpmnModelInstance.class));
    verify(versioningService).save(any(VersionedWorkflow.class));
  }

  @Test
  void testUpdate_latestNoPublishedVersion_publishSucceed() {
    when(conveter.convert(anyString(), eq(Workflow.class))).thenReturn(workflow);
    VersionedWorkflow latestNoPublishedVersion = new VersionedWorkflow();
    latestNoPublishedVersion.setPublished(false);
    when(versioningService.findFirstByWorkflowIdOrderByVersionDesc(anyString())).thenReturn(
        Optional.of(latestNoPublishedVersion));
    BpmnModelInstance instance = mock(BpmnModelInstance.class);
    when(camundaEngine.parseAndValidate(any(Workflow.class))).thenReturn(instance);
    when(camundaEngine.deploy(any(Workflow.class), any(BpmnModelInstance.class))).thenReturn("id");
    when(versioningService.save(any(VersionedWorkflow.class))).thenReturn(latestNoPublishedVersion);

    workflowManagementService.update(swadl);

    assertThat(latestNoPublishedVersion.getDeploymentId()).isEqualTo("id");
    assertThat(latestNoPublishedVersion.getActive()).isTrue();
    assertThat(latestNoPublishedVersion.getPublished()).isTrue();
    verify(camundaEngine).deploy(any(Workflow.class), any(BpmnModelInstance.class));
  }

  @Test
  void testUpdate_latestNoPublishedVersion_updateOnlySucceed() {
    Properties properties = new Properties();
    properties.setPublish(false);
    workflow.setProperties(properties);
    when(conveter.convert(anyString(), eq(Workflow.class))).thenReturn(workflow);
    VersionedWorkflow latestNoPublishedVersion = new VersionedWorkflow();
    latestNoPublishedVersion.setPublished(false);
    latestNoPublishedVersion.setActive(false);
    when(versioningService.findFirstByWorkflowIdOrderByVersionDesc(anyString())).thenReturn(
        Optional.of(latestNoPublishedVersion));
    BpmnModelInstance instance = mock(BpmnModelInstance.class);
    when(camundaEngine.parseAndValidate(any(Workflow.class))).thenReturn(instance);
    when(versioningService.save(any(VersionedWorkflow.class))).thenReturn(latestNoPublishedVersion);

    workflowManagementService.update(swadl);

    assertThat(latestNoPublishedVersion.getActive()).isFalse();
    assertThat(latestNoPublishedVersion.getPublished()).isFalse();
    verify(camundaEngine, never()).deploy(any(Workflow.class), any(BpmnModelInstance.class));
  }

  @Test
  void testUpdate_noActiveVersion_notFoundException() {
    when(conveter.convert(anyString(), eq(Workflow.class))).thenReturn(workflow);
    when(versioningService.findFirstByWorkflowIdOrderByVersionDesc(anyString())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> workflowManagementService.update(swadl)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void testDelete_existingVersion_succeed() {
    doNothing().when(camundaEngine).undeploy(anyString());
    doNothing().when(versioningService).deleteByWorkflowId(anyString());
    when(versioningService.existsByWorkflowId(anyString())).thenReturn(true);

    workflowManagementService.delete("id");

    verify(camundaEngine).undeploy(anyString());
    verify(versioningService).deleteByWorkflowId(anyString());
  }

  @Test
  void testDelete_noExistingWorkflow_notFoundException() {
    when(versioningService.existsByWorkflowId(anyString())).thenReturn(false);

    assertThatThrownBy(() -> workflowManagementService.delete("id")).isInstanceOf(NotFoundException.class);
  }

  @Test
  void testSetActiveVersion_switchActiveWorkflow() {
    String workflowId = "workflowId";
    VersionedWorkflow versionedWorkflow = new VersionedWorkflow();
    versionedWorkflow.setWorkflowId(workflowId);
    versionedWorkflow.setSwadl(swadl);
    versionedWorkflow.setPublished(true);
    VersionedWorkflow activeWorkflow = new VersionedWorkflow();
    activeWorkflow.setActive(true);

    when(versioningService.findByWorkflowIdAndVersion(anyString(), anyLong())).thenReturn(
        Optional.of(versionedWorkflow));
    when(versioningService.findByWorkflowIdAndActiveTrue(anyString())).thenReturn(Optional.of(activeWorkflow));
    when(conveter.convert(anyString(), eq(Workflow.class))).thenReturn(workflow);
    String deploymentId = "ABC";
    when(camundaEngine.deploy(any())).thenReturn(deploymentId);
    when(versioningService.save(any())).thenReturn(versionedWorkflow);
    when(versioningService.saveAndFlush(any())).thenReturn(activeWorkflow);

    workflowManagementService.setActiveVersion(workflowId, "1234");

    verify(camundaEngine).deploy(any());
    verify(versioningService).save(any());
    assertThat(activeWorkflow.getActive()).isFalse();
    assertThat(versionedWorkflow.getActive()).isTrue();
    assertThat(versionedWorkflow.getDeploymentId()).isEqualTo(deploymentId);
  }

  @Test
  void testSetActiveVersion_workflowNotFound() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      workflowManagementService.setActiveVersion("notFoundWorkflowId", "1234");
    }).satisfies(
        e -> assertThat(e.getMessage())
            .isEqualTo("Version 1234 of the workflow notFoundWorkflowId does not exist."));
  }

  @Test
  void testSetActiveVersion_workflowInDraft_illegalException() {
    VersionedWorkflow versionedWorkflow = new VersionedWorkflow();
    versionedWorkflow.setWorkflowId("inactiveWorkflow");
    versionedWorkflow.setSwadl(swadl);
    versionedWorkflow.setPublished(false);
    when(versioningService.findByWorkflowIdAndVersion(anyString(), anyLong())).thenReturn(
        Optional.of(versionedWorkflow));
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
      workflowManagementService.setActiveVersion("inactiveWorkflow", "1234");
    }).satisfies(
        e -> assertThat(e.getMessage())
            .isEqualTo("Version 1234 of the workflow inactiveWorkflow is in draft mode."));
  }
}
