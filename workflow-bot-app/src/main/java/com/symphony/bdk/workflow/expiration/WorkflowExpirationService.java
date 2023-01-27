package com.symphony.bdk.workflow.expiration;

import com.symphony.bdk.workflow.configuration.ConditionalOnPropertyNotEmpty;
import com.symphony.bdk.workflow.exception.NotFoundException;
import com.symphony.bdk.workflow.versioning.model.WorkflowExpirationJob;
import com.symphony.bdk.workflow.versioning.repository.VersionedWorkflowRepository;
import com.symphony.bdk.workflow.versioning.repository.WorkflowExpirationJobRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Component
@ConditionalOnPropertyNotEmpty("wdk.properties.management-token")
@Transactional(propagation = Propagation.REQUIRES_NEW)
@RequiredArgsConstructor
@Slf4j
public class WorkflowExpirationService {
  private static final String WORKFLOW_NOT_EXIST_EXCEPTION_MSG = "Workflow %s does not exist.";
  private final WorkflowExpirationJobRepository expirationJobRepository;
  private final VersionedWorkflowRepository versioningRepository;
  private final WorkflowExpirationPlanner workflowExpirationPlanner;

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
    expirationJobs.forEach(this.workflowExpirationPlanner::planExpiration);
  }
}
