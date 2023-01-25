package com.symphony.bdk.workflow.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowInstView {
  private String id;
  private Integer version;
  private String instanceId;
  private InstanceStatusEnum status;
  private Instant startDate;
  private Instant endDate;
  private Duration duration;
}
