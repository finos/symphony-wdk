package com.symphony.bdk.workflow.expiration;

import com.symphony.bdk.workflow.configuration.ConditionalOnPropertyNotEmpty;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.engine.camunda.CamundaTranslatedWorkflowContext;
import com.symphony.bdk.workflow.scheduled.RunnableScheduledJob;
import com.symphony.bdk.workflow.scheduled.ScheduledJobsRegistry;
import com.symphony.bdk.workflow.versioning.model.WorkflowExpirationJob;
import com.symphony.bdk.workflow.versioning.repository.VersionedWorkflowRepository;
import com.symphony.bdk.workflow.versioning.repository.WorkflowExpirationJobRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@ConditionalOnPropertyNotEmpty("wdk.properties.management-token")
public class DefaultWorkflowExpirationPlanner implements WorkflowExpirationPlanner {
  private final WorkflowEngine<CamundaTranslatedWorkflowContext> workflowEngine;
  private final WorkflowExpirationJobRepository expirationJobRepository;
  private final VersionedWorkflowRepository versioningRepository;
  private final ScheduledJobsRegistry scheduledJobsRegistry;

  @Override
  public void planExpiration(WorkflowExpirationJob workflowExpirationJob) {
    scheduledJobsRegistry.scheduleJob(new RunnableScheduledJob(
        () -> String.format("%s.%s", workflowExpirationJob.getWorkflowId(), workflowExpirationJob.getExpirationDate()),
        Duration.between(Instant.now(), workflowExpirationJob.getExpirationDate()).getSeconds(),
        () -> {
          versioningRepository.deleteById(workflowExpirationJob.getId());
          expirationJobRepository.deleteById(workflowExpirationJob.getId());
          workflowEngine.undeployByDeploymentId(workflowExpirationJob.getDeploymentId());
        }));
  }
}
