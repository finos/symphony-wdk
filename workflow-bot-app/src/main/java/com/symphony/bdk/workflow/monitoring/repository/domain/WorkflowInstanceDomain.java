package com.symphony.bdk.workflow.monitoring.repository.domain;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;

@Data
@Builder
public class WorkflowInstanceDomain {
  private final String id;
  private final String name;
  private final Long version;
  private final String instanceId;
  private final String status;
  private final Instant startDate;
  private final Instant endDate;
  private final Duration duration;
}
