package com.symphony.bdk.workflow.swadl.v1.event;

import lombok.Data;

@Data
public class TimerFiredEvent {
  private String at;
  private String repeat;
}
