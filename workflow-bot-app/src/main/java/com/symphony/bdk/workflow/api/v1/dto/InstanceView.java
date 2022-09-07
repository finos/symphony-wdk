package com.symphony.bdk.workflow.api.v1.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InstanceView {
  private String workflowId;
  private Integer workflowVersion;
  private String instanceId;
  private InstanceStatusEnum status;
  private Long startDate;
  private Long endDate;
}
