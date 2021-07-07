package com.symphony.bdk.workflow.lang.swadl.activity;

import com.symphony.bdk.workflow.lang.swadl.Event;

import lombok.Data;

// TODO to implement part of PLAT-11085
@Data
public class SendMessage {
  private String name;
  private Event on;
}
