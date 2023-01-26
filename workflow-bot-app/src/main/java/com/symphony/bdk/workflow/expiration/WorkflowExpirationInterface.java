package com.symphony.bdk.workflow.expiration;

import com.symphony.bdk.workflow.versioning.model.WorkflowExpirationJob;

import java.time.Instant;

public interface WorkflowExpirationInterface {
  void scheduleWorkflowExpiration(String workflowId, Instant instant);

  void scheduleJob(WorkflowExpirationJob workflowExpirationJob);
}
