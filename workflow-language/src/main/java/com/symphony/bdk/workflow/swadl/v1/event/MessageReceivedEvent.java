package com.symphony.bdk.workflow.swadl.v1.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MessageReceivedEvent extends InnerEvent {
  @JsonProperty
  private String content = "";
  @JsonProperty
  private boolean requiresBotMention = false;
}
