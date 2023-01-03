package com.symphony.bdk.workflow.management;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowMgtAction;
import com.symphony.bdk.workflow.engine.camunda.CamundaEngine;
import com.symphony.bdk.workflow.exception.NotFoundException;
import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;
import com.symphony.bdk.workflow.versioning.service.VersioningService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class WorkflowManagementServiceTest {

  @Mock
  VersioningService versioningService;

  @Mock
  WorkflowsMgtActionHolder holder;

  @Mock
  CamundaEngine camundaEngine;

  @Mock
  WorkflowDeployAction deployAction;

  @Mock
  WorkflowUpdateAction updateAction;

  @Mock
  WorkflowDeleteAction deleteAction;

  @InjectMocks
  WorkflowManagementService workflowManagementService;

  private static final String SWADL = "id: wfexample\n"
      + "properties:\n"
      + "  version: version1\n"
      + "\n"
      + "activities:\n"
      + "  - execute-script:\n"
      + "      id: script\n"
      + "      on:\n"
      + "        message-received:\n"
      + "          content: /go\n"
      + "      script: |\n";

  @Test
  void testDeploy() {
    when(holder.getInstance(WorkflowMgtAction.DEPLOY)).thenReturn(deployAction);
    doNothing().when(deployAction).doAction(anyString());

    workflowManagementService.deploy("content");

    verify(deployAction).doAction(eq("content"));
  }

  @Test
  void testUpdate() {
    when(holder.getInstance(WorkflowMgtAction.UPDATE)).thenReturn(updateAction);
    doNothing().when(updateAction).doAction(anyString());

    workflowManagementService.update("content");

    verify(updateAction).doAction(eq("content"));
  }

  @Test
  void testDelete() {
    when(holder.getInstance(WorkflowMgtAction.DELETE)).thenReturn(deleteAction);
    doNothing().when(deleteAction).doAction(anyString());

    workflowManagementService.delete("content");

    verify(deleteAction).doAction(eq("content"));
  }

  @Test
  void testSetActiveVersion() {
    final String workflowId = "workflowId";
    final String version = "v1";
    final String deploymentId = "ABC";
    final VersionedWorkflow versionedWorkflow =
        new VersionedWorkflow().setWorkflowId(workflowId).setVersion(version).setSwadl(SWADL);

    when(versioningService.findByWorkflowIdAndVersion(anyString(), anyString())).thenReturn(
        Optional.of(versionedWorkflow));
    when(camundaEngine.deploy(any())).thenReturn(deploymentId);
    doNothing().when(versioningService).save(any(VersionedWorkflow.class), anyString());

    workflowManagementService.setActiveVersion(workflowId, version);

    verify(versioningService).findByWorkflowIdAndVersion(eq(workflowId), eq(version));
    verify(camundaEngine).deploy(any());
    verify(versioningService).save(eq(versionedWorkflow), eq(deploymentId));
  }

  @Test
  void testSetActiveVersionWorkflowNotFound() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      workflowManagementService.setActiveVersion("notFoundWorkflowId", "v1");
    }).satisfies(
        e -> assertThat(e.getMessage())
                .isEqualTo("Version v1 of the workflow notFoundWorkflowId does not exist"));
  }

}
