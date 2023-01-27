package com.symphony.bdk.workflow.expiration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.workflow.exception.NotFoundException;
import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;
import com.symphony.bdk.workflow.versioning.model.WorkflowExpirationJob;
import com.symphony.bdk.workflow.versioning.repository.VersionedWorkflowRepository;
import com.symphony.bdk.workflow.versioning.repository.WorkflowExpirationJobRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class WorkflowExpirationServiceTest {
  @Mock
  private WorkflowExpirationJobRepository workflowExpirationJobRepository;

  @Mock
  private VersionedWorkflowRepository versionedWorkflowRepository;

  @Mock
  private WorkflowExpirationPlanner workflowExpirationPlanner;

  @InjectMocks
  private WorkflowExpirationService workflowExpirationService;

  @Test
  void scheduleWorkflowExpirationTest() {
    final String workflowId = "workflowId";
    final String id = "id";
    final String deploymentId = "deploymentId";
    final Instant now = Instant.now();

    final VersionedWorkflow versionedWorkflow = new VersionedWorkflow();
    versionedWorkflow.setWorkflowId(workflowId);
    versionedWorkflow.setId(id);
    versionedWorkflow.setDeploymentId(deploymentId);

    when(versionedWorkflowRepository.findByWorkflowId(eq(workflowId))).thenReturn(
        Collections.singletonList(versionedWorkflow));

    ArgumentCaptor<List> expirationJobsCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<WorkflowExpirationJob> workflowExpirationJobArgumentCaptor =
        ArgumentCaptor.forClass(WorkflowExpirationJob.class);

    when(workflowExpirationJobRepository.saveAll(expirationJobsCaptor.capture())).thenReturn(null);
    doNothing().when(workflowExpirationPlanner).planExpiration(workflowExpirationJobArgumentCaptor.capture());

    workflowExpirationService.scheduleWorkflowExpiration(workflowId, now);

    WorkflowExpirationJob expected = new WorkflowExpirationJob();
    expected.setId(id);
    expected.setWorkflowId(workflowId);
    expected.setDeploymentId(deploymentId);
    expected.setExpirationDate(now);

    assertThat(expirationJobsCaptor.getValue()).hasSize(1);
    assertThat(expirationJobsCaptor.getValue().get(0)).isEqualTo(expected);
    verify(workflowExpirationPlanner).planExpiration(any());
    assertThat(workflowExpirationJobArgumentCaptor.getValue()).isEqualTo(expected);
  }

  @Test
  void scheduleWorkflowExpiration_workflowNotFound() {
    final String workflowId = "workflowId";

    when(versionedWorkflowRepository.findByWorkflowId(eq(workflowId))).thenReturn(Collections.emptyList());

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
            () -> workflowExpirationService.scheduleWorkflowExpiration(workflowId, Instant.now()))
        .satisfies(e -> assertThat(e.getMessage()).isEqualTo(String.format("Workflow %s does not exist.", workflowId)));
  }
}
