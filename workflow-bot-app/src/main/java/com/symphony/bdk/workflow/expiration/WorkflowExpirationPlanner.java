package com.symphony.bdk.workflow.expiration;

import com.symphony.bdk.workflow.management.repository.domain.WorkflowExpirationJob;

public interface WorkflowExpirationPlanner {
  void planExpiration(WorkflowExpirationJob workflowExpirationJob);
}
