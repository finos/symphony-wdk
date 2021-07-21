package com.symphony.bdk.workflow.event;

import com.symphony.bdk.gen.api.model.V4SymphonyElementsAction;
import com.symphony.bdk.spring.events.RealTimeEvent;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DatafeedSymphonyElementsEventToWorkflowEvent extends DatafeedEventToWorkflowEvent {

  @EventListener
  public void onSymphonyElementsAction(RealTimeEvent<V4SymphonyElementsAction> event) {
    LOGGER.info("Triggered Symphony Elements Action event {}", event.getSource().getStream().getStreamId());
    Map<String, Object> formReplies = (Map<String, Object>) event.getSource().getFormValues();
    String formId = event.getSource().getFormId();
    workflowEngine.formReceived(event.getSource().getFormMessageId(), formId, formReplies);
  }
}
