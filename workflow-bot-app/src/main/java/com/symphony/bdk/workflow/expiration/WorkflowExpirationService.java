package com.symphony.bdk.workflow.expiration;

import com.symphony.bdk.workflow.configuration.ConditionalOnPropertyNotEmpty;
import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.exception.NotFoundException;
import com.symphony.bdk.workflow.management.WorkflowManagementService;
import com.symphony.bdk.workflow.scheduled.RunnableScheduledJob;
import com.symphony.bdk.workflow.scheduled.ScheduledJobsRegistry;
import com.symphony.bdk.workflow.versioning.model.WorkflowExpirationJob;
import com.symphony.bdk.workflow.versioning.repository.VersionedWorkflowRepository;
import com.symphony.bdk.workflow.versioning.repository.WorkflowExpirationJobRepository;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Component
@ConditionalOnPropertyNotEmpty("wdk.properties.management-token")
@Transactional(propagation = Propagation.REQUIRES_NEW)
@Slf4j
public class WorkflowExpirationService extends WorkflowManagementService implements WorkflowExpirationInterface {
  private final WorkflowExpirationJobRepository expirationJobRepository;
  private final ScheduledJobsRegistry scheduledJobsRegistry;

  public WorkflowExpirationService(WorkflowExpirationJobRepository workflowExpirationJobRepository,
      ScheduledJobsRegistry scheduledJobsRegistry, WorkflowEngine<BpmnModelInstance> workflowEngine,
      VersionedWorkflowRepository versionedWorkflowRepository, ObjectConverter objectConverter) {
    super(workflowEngine, versionedWorkflowRepository, objectConverter);
    this.expirationJobRepository = workflowExpirationJobRepository;
    this.scheduledJobsRegistry = scheduledJobsRegistry;
  }

  @Override
  public void scheduleWorkflowExpiration(String workflowId, Instant instant) {
    List<WorkflowExpirationJob> expirationJobs = versioningRepository.findByWorkflowId(workflowId)
        .stream()
        .map(workflow -> new WorkflowExpirationJob(workflow.getId(), workflow.getWorkflowId(),
            workflow.getDeploymentId(), instant))
        .collect(Collectors.toList());

    if (expirationJobs.isEmpty()) {
      throw new NotFoundException(String.format(WORKFLOW_NOT_EXIST_EXCEPTION_MSG, workflowId));
    }

    expirationJobRepository.saveAll(expirationJobs);
    expirationJobs.forEach(this::scheduleJob);
  }

  @Override
  public void scheduleJob(WorkflowExpirationJob workflowExpirationJob) {
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
