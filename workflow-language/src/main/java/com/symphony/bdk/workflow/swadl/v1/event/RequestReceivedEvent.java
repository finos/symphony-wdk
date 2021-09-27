package com.symphony.bdk.workflow.swadl.v1.event;

import lombok.Data;

import java.util.Map;

/**
 * Not a Datafeed event. Manually created when API calls are made to the bot directly.
 */
@Data
public class RequestReceivedEvent {
  private String token;
  private Map<String, Object> arguments;
  /**
   * To be able to match a specific workflow when the event is received.
   */
  private String workflowId;
}
