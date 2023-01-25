package com.symphony.bdk.workflow.expiration;

import com.symphony.bdk.workflow.api.v1.dto.DeploymentExpirationEnum;

import java.time.Instant;

public interface WorkflowExpirationInterface {
  void scheduleWorkflowExpiration(String workflowId, String type, Instant instant);

  void extracted(String id, String deploymentId, String workflowId, Instant instant,
      DeploymentExpirationEnum deploymentExpirationEnum);
}
