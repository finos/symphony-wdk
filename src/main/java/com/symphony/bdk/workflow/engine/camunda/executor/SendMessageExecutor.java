package com.symphony.bdk.workflow.engine.camunda.executor;

import com.symphony.bdk.core.service.message.MessageService;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SendMessageExecutor implements JavaDelegate {
  private static final Logger LOGGER = LoggerFactory.getLogger(SendMessageExecutor.class);

  private final MessageService messageService;

  public SendMessageExecutor(MessageService messageService) {
    this.messageService = messageService;
  }

  @Override
  public void execute(DelegateExecution execution) {
    LOGGER.info("Running send message");
    // TODO to be implemented
    messageService.send("123", "message");
  }
}
