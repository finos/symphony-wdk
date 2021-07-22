package com.symphony.bdk.workflow.event;

import com.symphony.bdk.core.service.message.exception.PresentationMLParserException;
import com.symphony.bdk.core.service.message.util.PresentationMLParser;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.gen.api.model.V4SymphonyElementsAction;
import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.lang.validator.YamlValidator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DatafeedEventToWorkflowEvent {

  @Autowired
  private WorkflowEngine workflowEngine;

  @EventListener
  public void onMessageSent(RealTimeEvent<V4MessageSent> event) throws PresentationMLParserException {
    if (!PresentationMLParser.getTextContent(event.getSource().getMessage().getMessage())
        .equals(YamlValidator.YAML_VALIDATION_COMMAND)) {
      String streamId = event.getSource().getMessage().getStream().getStreamId();
      String content = PresentationMLParser.getTextContent(event.getSource().getMessage().getMessage());
      workflowEngine.messageReceived(streamId, content);
    }
  }

  @EventListener
  public void onSymphonyElementsAction(RealTimeEvent<V4SymphonyElementsAction> event) {
    Map<String, Object> formReplies = (Map<String, Object>) event.getSource().getFormValues();
    String formId = event.getSource().getFormId();
    workflowEngine.formReceived(event.getSource().getFormMessageId(), formId, formReplies);
  }

}
