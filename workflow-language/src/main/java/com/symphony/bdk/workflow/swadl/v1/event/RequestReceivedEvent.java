package com.symphony.bdk.workflow.swadl.v1.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * Not a Datafeed event. Manually created when API calls are made to the bot directly.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RequestReceivedEvent extends InnerEvent {
  @JsonProperty
  private String token;

  @JsonProperty
  private Map<String, Object> arguments;

  /**
   * To be able to match a specific workflow when the event is received.
   */
  @JsonProperty
  private String workflowId;
}
