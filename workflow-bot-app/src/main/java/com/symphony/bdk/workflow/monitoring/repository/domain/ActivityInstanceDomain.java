package com.symphony.bdk.workflow.monitoring.repository.domain;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;

@Data
@Builder
public class ActivityInstanceDomain {
  private final String id;
  private final String name;
  private final String procInstId;
  private final String workflowId;
  private final String type;
  private final Instant startDate;
  private final Instant endDate;
  private final Duration duration;
  private VariablesDomain variables;
}
