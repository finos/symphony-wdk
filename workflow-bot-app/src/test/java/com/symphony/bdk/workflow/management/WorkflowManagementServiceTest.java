package com.symphony.bdk.workflow.management;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.workflow.api.v1.dto.SwadlView;
import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.engine.camunda.CamundaTranslatedWorkflowContext;
import com.symphony.bdk.workflow.exception.NotFoundException;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Properties;
import com.symphony.bdk.workflow.swadl.v1.Workflow;
import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;
import com.symphony.bdk.workflow.versioning.repository.VersionedWorkflowRepository;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class WorkflowManagementServiceTest {

  @Mock
  VersionedWorkflowRepository versionRepository;

  @Mock
  ObjectConverter conveter;

  @Mock
  WorkflowEngine<CamundaTranslatedWorkflowContext> camundaEngine;

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
  SwadlView swadlView;

  public WorkflowManagementServiceTest() throws IOException, ProcessingException {
    workflow = SwadlParser.fromYaml(swadl);
    swadlView = SwadlView.builder().swadl(swadl).description("desc").build();
  }

  @Test
  void testDeploy_existingActiveVersion_updateOldInsertNew() {
    when(conveter.convert(anyString(), eq(Workflow.class))).thenReturn(workflow);
    CamundaTranslatedWorkflowContext context = mock(CamundaTranslatedWorkflowContext.class);
    when(context.getWorkflow()).thenReturn(workflow);
    when(camundaEngine.translate(any(Workflow.class))).thenReturn(context);
    when(camundaEngine.deploy(any(CamundaTranslatedWorkflowContext.class))).thenReturn("id");
    VersionedWorkflow activeVersion = new VersionedWorkflow();
    when(versionRepository.save(any())).thenReturn(activeVersion);
    when(versionRepository.saveAndFlush(any())).thenReturn(activeVersion);
    when(versionRepository.findByWorkflowIdAndActiveTrue(eq("test"))).thenReturn(Optional.of(activeVersion));

    workflowManagementService.deploy(swadlView);

    assertThat(activeVersion.getActive()).isFalse();
    verify(camundaEngine).deploy(any(CamundaTranslatedWorkflowContext.class));
    verify(versionRepository).save(any());
    verify(versionRepository).saveAndFlush(any());
  }

  @Test
  void testDeploy_noActiveVersion_insertNew() {
    when(conveter.convert(anyString(), eq(Workflow.class))).thenReturn(workflow);
    CamundaTranslatedWorkflowContext context = mock(CamundaTranslatedWorkflowContext.class);
    when(context.getWorkflow()).thenReturn(workflow);
    when(camundaEngine.translate(any(Workflow.class))).thenReturn(context);
    when(camundaEngine.deploy(any(CamundaTranslatedWorkflowContext.class))).thenReturn("id");
    when(versionRepository.save(any(VersionedWorkflow.class))).thenReturn(null);
    when(versionRepository.findByWorkflowIdAndActiveTrue(eq("test"))).thenReturn(Optional.empty());

    workflowManagementService.deploy(swadlView);

    verify(camundaEngine).deploy(any(CamundaTranslatedWorkflowContext.class));
    verify(versionRepository).save(any(VersionedWorkflow.class));
  }

  @Test
  void testDeploy_existNoPublishedVersion_exceptionThrown() {
    when(conveter.convert(anyString(), eq(Workflow.class))).thenReturn(workflow);
    CamundaTranslatedWorkflowContext context = mock(CamundaTranslatedWorkflowContext.class);
    when(camundaEngine.translate(any(Workflow.class))).thenReturn(context);
    VersionedWorkflow noPublishedVersion = new VersionedWorkflow();
    noPublishedVersion.setPublished(false);
    noPublishedVersion.setVersion(1234L);
    when(versionRepository.findByWorkflowIdAndPublishedFalse(anyString())).thenReturn(Optional.of(noPublishedVersion));

    assertThatThrownBy(() -> workflowManagementService.deploy(swadlView)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Version 1234 of workflow has not published yet.");
  }

  @Test
  void testUpdate_latestNoPublishedVersion_publishSucceed() {
    when(conveter.convert(anyString(), eq(Workflow.class))).thenReturn(workflow);
    VersionedWorkflow noPublishedVersion = new VersionedWorkflow();
    noPublishedVersion.setPublished(false);
    when(versionRepository.findByWorkflowIdAndPublishedFalse(anyString())).thenReturn(Optional.of(noPublishedVersion));
    CamundaTranslatedWorkflowContext context = mock(CamundaTranslatedWorkflowContext.class);
    when(camundaEngine.translate(any(Workflow.class))).thenReturn(context);
    when(camundaEngine.deploy(any(CamundaTranslatedWorkflowContext.class))).thenReturn("id");
    when(versionRepository.save(any(VersionedWorkflow.class))).thenReturn(noPublishedVersion);

    workflowManagementService.update(swadlView);

    assertThat(noPublishedVersion.getDeploymentId()).isEqualTo("id");
    assertThat(noPublishedVersion.getActive()).isTrue();
    assertThat(noPublishedVersion.getPublished()).isTrue();
    verify(camundaEngine).deploy(any(CamundaTranslatedWorkflowContext.class));
  }

  @Test
  void testUpdate_latestNoPublishedVersion_updateOnlySucceed() {
    Properties properties = new Properties();
    properties.setPublish(false);
    workflow.setProperties(properties);
    when(conveter.convert(anyString(), eq(Workflow.class))).thenReturn(workflow);
    VersionedWorkflow noPublishedVersion = new VersionedWorkflow();
    noPublishedVersion.setPublished(false);
    noPublishedVersion.setActive(false);
    when(versionRepository.findByWorkflowIdAndPublishedFalse(anyString())).thenReturn(Optional.of(noPublishedVersion));
    CamundaTranslatedWorkflowContext context = mock(CamundaTranslatedWorkflowContext.class);
    when(camundaEngine.translate(any(Workflow.class))).thenReturn(context);
    when(versionRepository.save(any(VersionedWorkflow.class))).thenReturn(noPublishedVersion);

    workflowManagementService.update(swadlView);

    assertThat(noPublishedVersion.getActive()).isFalse();
    assertThat(noPublishedVersion.getPublished()).isFalse();
    verify(camundaEngine, never()).deploy(any(CamundaTranslatedWorkflowContext.class));
  }

  @Test
  void testUpdate_noActiveVersion_notFoundException() {
    when(conveter.convert(anyString(), eq(Workflow.class))).thenReturn(workflow);
    when(versionRepository.findByWorkflowIdAndPublishedFalse(anyString())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> workflowManagementService.update(swadlView)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void testDelete_existingVersion_succeed() {
    VersionedWorkflow workflow = new VersionedWorkflow();
    workflow.setActive(true);
    workflow.setWorkflowId("id");
    workflow.setDeploymentId("deploymentId");
    doNothing().when(camundaEngine).undeployByDeploymentId(anyString());
    doNothing().when(versionRepository).deleteByWorkflowIdAndVersion(anyString(), anyLong());
    when(versionRepository.findByWorkflowIdAndVersion(anyString(), anyLong())).thenReturn(Optional.of(workflow));

    workflowManagementService.delete("id", 1674651222294886L);

    verify(camundaEngine).undeployByDeploymentId(anyString());
    verify(versionRepository).deleteByWorkflowIdAndVersion(anyString(), anyLong());
  }

  @Test
  void testDelete_withoutVersion_succeed() {
    doNothing().when(camundaEngine).undeployByWorkflowId(anyString());
    doNothing().when(versionRepository).deleteByWorkflowId(anyString());

    workflowManagementService.delete("id", null);

    verify(camundaEngine).undeployByWorkflowId(anyString());
    verify(versionRepository).deleteByWorkflowId(anyString());
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

    when(versionRepository.findByWorkflowIdAndVersion(anyString(), anyLong())).thenReturn(
        Optional.of(versionedWorkflow));
    when(versionRepository.findByWorkflowIdAndActiveTrue(anyString())).thenReturn(Optional.of(activeWorkflow));
    when(conveter.convert(anyString(), eq(Workflow.class))).thenReturn(workflow);
    String deploymentId = "ABC";
    when(camundaEngine.deploy(any(Workflow.class))).thenReturn(deploymentId);
    when(versionRepository.save(any())).thenReturn(versionedWorkflow);
    when(versionRepository.saveAndFlush(any())).thenReturn(activeWorkflow);

    workflowManagementService.setActiveVersion(workflowId, 1674651222294886L);

    verify(camundaEngine).deploy(any(Workflow.class));
    verify(versionRepository).save(any());
    assertThat(activeWorkflow.getActive()).isFalse();
    assertThat(versionedWorkflow.getActive()).isTrue();
    assertThat(versionedWorkflow.getDeploymentId()).isEqualTo(deploymentId);
  }

  @Test
  void testSetActiveVersion_workflowNotFound() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      workflowManagementService.setActiveVersion("notFoundWorkflowId", 1674651222294886L);
    }).satisfies(
        e -> assertThat(e.getMessage())
            .isEqualTo("Version 1674651222294886 of the workflow notFoundWorkflowId does not exist."));
  }

  @Test
  void testSetActiveVersion_workflowInDraft_illegalException() {
    VersionedWorkflow versionedWorkflow = new VersionedWorkflow();
    versionedWorkflow.setWorkflowId("inactiveWorkflow");
    versionedWorkflow.setSwadl(swadl);
    versionedWorkflow.setPublished(false);
    when(versionRepository.findByWorkflowIdAndVersion(anyString(), anyLong())).thenReturn(
        Optional.of(versionedWorkflow));
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
      workflowManagementService.setActiveVersion("inactiveWorkflow", 1674651222294886L);
    }).satisfies(
        e -> assertThat(e.getMessage())
            .isEqualTo("Version 1674651222294886 of the workflow inactiveWorkflow is in draft mode."));
  }
}
