package com.symphony.bdk.workflow.lang.swadl.event;

import lombok.Data;

@Data
public class MessageReceivedEvent {
  private String id;
  private String content;
  // TODO add mention flag
}
