package com.symphony.bdk.workflow.swadl.v1.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ActivityCompletedEvent {
  private String activityId;

  @JsonProperty("if")
  private String ifCondition;
}
