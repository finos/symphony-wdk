package com.symphony.bdk.workflow.swadl.v1.event;

import lombok.Data;

import java.util.Map;

@Data
public class RequestReceivedEvent {
  private Map<String, Object> bodyArguments;
  private String token;
}
