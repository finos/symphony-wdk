package com.symphony.bdk.workflow.event;

import com.symphony.bdk.gen.api.model.V4ConnectionAccepted;
import com.symphony.bdk.gen.api.model.V4ConnectionRequested;
import com.symphony.bdk.spring.events.RealTimeEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DatafeedConnectionEventToWorkflowEvent extends DatafeedEventToWorkflowEvent{

  private static final Logger LOGGER = LoggerFactory.getLogger(DatafeedConnectionEventToWorkflowEvent.class);

  @EventListener
  public void onConnectionRequested(RealTimeEvent<V4ConnectionRequested> event) {
    LOGGER.info("Triggered connection requested event to user {}", event.getSource().getToUser().getUserId());
    workflowEngine.connectionRequested(event);
  }

  @EventListener
  public void onConnectionAccepted(RealTimeEvent<V4ConnectionAccepted> event) {
    LOGGER.info("Triggered connection accepted event by user {}", event.getInitiator().getUser().getUserId());
    workflowEngine.connectionAccepted(event);
  }
}
