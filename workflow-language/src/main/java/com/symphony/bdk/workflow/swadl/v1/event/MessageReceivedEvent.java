package com.symphony.bdk.workflow.swadl.v1.event;

import lombok.Data;

@Data
public class MessageReceivedEvent {
  private String content = "";
  private boolean requiresBotMention = false;
}
