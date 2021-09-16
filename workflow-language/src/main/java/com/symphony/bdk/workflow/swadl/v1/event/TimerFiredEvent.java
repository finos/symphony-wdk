package com.symphony.bdk.workflow.swadl.v1.event;

import lombok.Data;

import javax.annotation.Nullable;

@Data
public class TimerFiredEvent {
  @Nullable private String at;
  @Nullable private String repeat;
}
