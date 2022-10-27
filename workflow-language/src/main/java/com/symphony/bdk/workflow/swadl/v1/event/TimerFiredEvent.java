package com.symphony.bdk.workflow.swadl.v1.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.annotation.Nullable;

@Data
@EqualsAndHashCode(callSuper = true)
public class TimerFiredEvent extends InnerEvent {
  @JsonProperty
  @Nullable
  private String at;

  @JsonProperty
  @Nullable
  private String repeat;
}
