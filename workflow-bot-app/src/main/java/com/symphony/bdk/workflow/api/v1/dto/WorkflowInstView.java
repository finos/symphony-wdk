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
  private Long version;
  private String instanceId;
  private StatusEnum status;
  private Instant startDate;
  private Instant endDate;
  private Duration duration;
}
