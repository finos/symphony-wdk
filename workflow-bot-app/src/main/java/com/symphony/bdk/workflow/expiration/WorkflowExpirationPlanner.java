package com.symphony.bdk.workflow.expiration;

import com.symphony.bdk.workflow.versioning.model.WorkflowExpirationJob;

public interface WorkflowExpirationPlanner {
  void planExpiration(WorkflowExpirationJob workflowExpirationJob);
}
