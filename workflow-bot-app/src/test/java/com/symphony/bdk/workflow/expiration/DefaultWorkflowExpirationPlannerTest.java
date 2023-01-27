package com.symphony.bdk.workflow.expiration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.symphony.bdk.workflow.scheduled.RunnableScheduledJob;
import com.symphony.bdk.workflow.scheduled.ScheduledJobsRegistry;
import com.symphony.bdk.workflow.versioning.model.WorkflowExpirationJob;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

@ExtendWith(MockitoExtension.class)
public class DefaultWorkflowExpirationPlannerTest {

  @Mock
  private ScheduledJobsRegistry scheduledJobsRegistry;

  @InjectMocks
  private DefaultWorkflowExpirationPlanner defaultWorkflowExpirationPlanner;

  @Test
  void planExpirationTest() {
    WorkflowExpirationJob workflowExpirationJob = new WorkflowExpirationJob();
    workflowExpirationJob.setWorkflowId("workflowId");
    workflowExpirationJob.setId("id");
    workflowExpirationJob.setDeploymentId("deploymentId");
    workflowExpirationJob.setExpirationDate(Instant.now());

    defaultWorkflowExpirationPlanner.planExpiration(workflowExpirationJob);
    ArgumentCaptor<RunnableScheduledJob> captor = ArgumentCaptor.forClass(RunnableScheduledJob.class);

    verify(scheduledJobsRegistry).scheduleJob(captor.capture());
    assertThat(captor.getValue()).isNotNull();
  }

}
