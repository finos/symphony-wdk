package com.symphony.bdk.workflow.swadl.v1.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.annotation.Nullable;

@Data
public class TimerFiredEvent {
  @JsonProperty
  @Nullable
  private String at;

  @JsonProperty
  @Nullable
  private String repeat;
}
