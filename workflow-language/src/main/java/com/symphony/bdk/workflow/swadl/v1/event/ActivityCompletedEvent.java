package com.symphony.bdk.workflow.swadl.v1.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.annotation.Nullable;

@Data
@EqualsAndHashCode(callSuper = true)
public class ActivityCompletedEvent extends ActivityEvent {
  @JsonProperty("if")
  @Nullable
  private String ifCondition;
}
