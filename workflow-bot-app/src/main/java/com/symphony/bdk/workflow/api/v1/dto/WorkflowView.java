package com.symphony.bdk.workflow.api.v1.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowView {
  private String workflowId;
  private int version;
}
