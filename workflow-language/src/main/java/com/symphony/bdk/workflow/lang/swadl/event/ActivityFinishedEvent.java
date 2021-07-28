package com.symphony.bdk.workflow.lang.swadl.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ActivityFinishedEvent {
  private String activityId;
  @JsonProperty("if")
  private String ifCondition;
}
