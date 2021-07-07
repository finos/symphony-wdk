package com.symphony.bdk.workflow.event;

import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.context.WorkflowContext;
import com.symphony.bdk.workflow.context.WorkflowContextBuilder;
import com.symphony.bdk.workflow.engine.WorkflowEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class WorkflowBotController {

  private final WorkflowEngine workflowEngine;
  private final MessageService messageService;

  public WorkflowBotController(WorkflowEngine workflowEngine,
      MessageService messageService) {
    this.workflowEngine = workflowEngine;
    this.messageService = messageService;
  }

  @EventListener
  public void onMessageSent(RealTimeEvent<V4MessageSent> event) throws Exception {
    WorkflowContext context = new WorkflowContextBuilder().fromEvent(event).build();

    String messageMlExecutionResult = workflowEngine.execute(context);

    messageService.send(context.getStreamId(), messageMlExecutionResult);
  }
}
