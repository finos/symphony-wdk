package com.symphony.bdk.workflow.event;

import com.symphony.bdk.core.service.message.exception.PresentationMLParserException;
import com.symphony.bdk.core.service.message.util.PresentationMLParser;
import com.symphony.bdk.gen.api.model.V4InstantMessageCreated;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.gen.api.model.V4MessageSuppressed;
import com.symphony.bdk.gen.api.model.V4SharedPost;
import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.lang.validator.YamlValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DatafeedMessageEventToWorkflowEvent extends DatafeedEventToWorkflowEvent {

  @EventListener
  public void onMessageSent(RealTimeEvent<V4MessageSent> event) throws PresentationMLParserException {
    if (!PresentationMLParser.getTextContent(event.getSource().getMessage().getMessage())
        .equals(YamlValidator.YAML_VALIDATION_COMMAND)) {
      String streamId = event.getSource().getMessage().getStream().getStreamId();
      String content = PresentationMLParser.getTextContent(event.getSource().getMessage().getMessage());
      LOGGER.info("Triggered message sent event {}", streamId);
      workflowEngine.messageReceived(streamId, content);
    }
  }

  @EventListener
  public void onMessageSuppressed(RealTimeEvent<V4MessageSuppressed> event) {
    LOGGER.info("Triggered message suppressed event {}", event.getSource().getStream().getStreamId());
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onSharedPost(RealTimeEvent<V4SharedPost> event) {
    LOGGER.info("Triggered shared post event {}", event.getSource().getMessage().getMessageId());
    workflowEngine.onEvent(event);
  }

  @EventListener
  public void onInstantMessageCreated(RealTimeEvent<V4InstantMessageCreated> event) {
    LOGGER.info("Triggered IM created event {}", event.getSource().getStream().getStreamId());
    workflowEngine.onEvent(event);
  }
}
