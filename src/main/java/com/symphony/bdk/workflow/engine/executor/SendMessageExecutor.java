package com.symphony.bdk.workflow.engine.executor;


import com.symphony.bdk.workflow.lang.swadl.activity.SendMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendMessageExecutor implements ActivityExecutor<SendMessage> {
  private static final Logger LOGGER = LoggerFactory.getLogger(SendMessageExecutor.class);

  @Override
  public void execute(ActivityExecutorContext<SendMessage> execution) {
    LOGGER.info("Running send message to room with id {}", execution.getVariable("roomId"));
    // TODO to be implemented
    // This shows errors as it is not implemented yet
    execution.messages().send("123", "message");
  }
}
