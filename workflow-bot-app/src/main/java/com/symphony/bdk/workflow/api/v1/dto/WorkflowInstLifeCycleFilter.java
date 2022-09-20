package com.symphony.bdk.workflow.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class WorkflowInstLifeCycleFilter {
  private final Long startedBefore;
  private final Long startedAfter;
  private final Long finishedBefore;
  private final Long finishedAfter;
}
