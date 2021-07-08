package com.symphony.bdk.workflow.lang.swadl.activity;

import com.symphony.bdk.workflow.lang.swadl.Event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// TODO to implement part of PLAT-11085
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SendMessage {
  private String name;
  private Event on;
}
