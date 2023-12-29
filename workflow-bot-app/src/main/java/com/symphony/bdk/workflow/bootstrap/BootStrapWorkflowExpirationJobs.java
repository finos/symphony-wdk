package com.symphony.bdk.workflow.bootstrap;

import com.symphony.bdk.workflow.configuration.ConditionalOnPropertyNotEmpty;
import com.symphony.bdk.workflow.expiration.WorkflowExpirationPlanner;
import com.symphony.bdk.workflow.management.repository.WorkflowExpirationJobRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"!test"})
@ConditionalOnPropertyNotEmpty("wdk.properties.management-token")
public class BootStrapWorkflowExpirationJobs {
  private final WorkflowExpirationJobRepository expirationJobRepository;
  private final WorkflowExpirationPlanner workflowExpirationPlanner;

  @PostConstruct
  void setupWorkflowExpirationJobs() {
    this.expirationJobRepository.findAll().forEach(this.workflowExpirationPlanner::planExpiration);
  }
}
