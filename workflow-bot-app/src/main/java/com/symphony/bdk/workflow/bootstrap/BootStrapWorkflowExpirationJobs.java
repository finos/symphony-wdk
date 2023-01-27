package com.symphony.bdk.workflow.bootstrap;

import com.symphony.bdk.workflow.expiration.WorkflowExpirationPlanner;
import com.symphony.bdk.workflow.versioning.repository.WorkflowExpirationJobRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"!test"})
public class BootStrapWorkflowExpirationJobs {
  private final WorkflowExpirationJobRepository expirationJobRepository;
  private final WorkflowExpirationPlanner workflowExpirationPlanner;

  @PostConstruct
  void setupWorkflowExpirationJobs() {
    this.expirationJobRepository.findAll().forEach(this.workflowExpirationPlanner::planExpiration);
  }
}
