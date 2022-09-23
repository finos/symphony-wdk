package com.symphony.bdk.workflow.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class WorkflowInstLifeCycleFilter {
  private final String startedBefore;
  private final String startedAfter;
  private final String finishedBefore;
  private final String finishedAfter;
}
