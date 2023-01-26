package com.symphony.bdk.workflow.expiration;

import java.time.Instant;

public interface WorkflowExpirationInterface {
  void scheduleWorkflowExpiration(String workflowId, Instant instant);

  void extracted(String id, String deploymentId, String workflowId, Instant instant);
}
