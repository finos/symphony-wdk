package com.symphony.bdk.workflow.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@AllArgsConstructor
@Getter
public class WorkflowInstLifeCycleFilter {
  private final Instant startedBefore;
  private final Instant startedAfter;
  private final Instant finishedBefore;
  private final Instant finishedAfter;
}
