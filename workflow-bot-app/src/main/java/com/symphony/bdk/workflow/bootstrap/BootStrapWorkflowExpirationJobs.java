package com.symphony.bdk.workflow.bootstrap;

import com.symphony.bdk.workflow.api.v1.dto.DeploymentExpirationEnum;
import com.symphony.bdk.workflow.expiration.WorkflowExpirationInterface;
import com.symphony.bdk.workflow.versioning.repository.WorkflowExpirationJobRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
@Profile({"!test"})
public class BootStrapWorkflowExpirationJobs {
  private final WorkflowExpirationInterface workflowExpirationService;
  private final WorkflowExpirationJobRepository expirationJobRepository;


  public BootStrapWorkflowExpirationJobs(WorkflowExpirationInterface workflowExpirationService,
      WorkflowExpirationJobRepository workflowExpirationJobRepository) {
    this.workflowExpirationService = workflowExpirationService;
    this.expirationJobRepository = workflowExpirationJobRepository;
  }

  @PostConstruct
  void setupWorkflowExpirationJobs() {
    this.expirationJobRepository.findAll()
        .forEach(
            job -> this.workflowExpirationService.extracted(job.getId(), job.getDeploymentId(), job.getWorkflowId(),
                job.getExpirationDate(), DeploymentExpirationEnum.valueOf(job.getDeploymentExpirationType())));
  }

}
