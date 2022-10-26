package com.symphony.bdk.workflow.swadl.v1.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MessageReceivedEvent extends InnerEvent {
  @JsonProperty
  private String content = "";
  @JsonProperty
  private boolean requiresBotMention = false;
}
