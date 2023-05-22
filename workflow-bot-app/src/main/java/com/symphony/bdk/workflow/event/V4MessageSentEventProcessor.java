package com.symphony.bdk.workflow.event;

import com.symphony.bdk.core.service.message.exception.PresentationMLParserException;
import com.symphony.bdk.core.service.message.util.PresentationMLParser;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.EventHolder;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.impl.event.EventType;
import org.camunda.bpm.engine.runtime.EventSubscription;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class V4MessageSentEventProcessor extends AbstractRealTimeEventProcessor<V4MessageSent> {
  private static final AntPathMatcher MESSAGE_RECEIVED_CONTENT_MATCHER = new AntPathMatcher();

  public V4MessageSentEventProcessor(RuntimeService runtimeService) {
    super(runtimeService, WorkflowEventType.MESSAGE_RECEIVED.getEventName());
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  protected void processEventSource(V4MessageSent eventSource, Map<String, Object> variables)
      throws PresentationMLParserException {
    // Event's message cannot be null, this if statement is only added to fix Sonar warnings
    if (eventSource.getMessage() != null) {
      log.debug("received message [{}]", eventSource.getMessage().getMessageId());
      log.trace("received message [{}]", eventSource.getMessage().getMessage());
      String presentationMl = eventSource.getMessage().getMessage();
      String receivedContent = PresentationMLParser.getTextContent(presentationMl);

      runtimeService.createMessageCorrelation(eventName + receivedContent)
          .setVariables(variables)
          .correlateAll();

      List<EventSubscription> subscribedSignals = runtimeService.createEventSubscriptionQuery()
          .eventType(EventType.SIGNAL.name())
          .list();

      // we want to avoid sending the same signals twice otherwise workflows would be triggered multiple times
      // meaning only the first matching /command is picked
      Set<String> alreadySentSignals = new HashSet<>();
      for (EventSubscription signal : subscribedSignals) {
        if (!alreadySentSignals.contains(signal.getEventName())) {
          String content = signal.getEventName().replace(eventName, "");
          if (MESSAGE_RECEIVED_CONTENT_MATCHER.match(content, receivedContent)) {
            // match the arguments and add them to the event holder
            Map<String, String> args =
                MESSAGE_RECEIVED_CONTENT_MATCHER.extractUriTemplateVariables(content, receivedContent);
            args.put(EVENT_NAME_KEY, signal.getEventName());
            ((EventHolder) variables.get(ActivityExecutorContext.EVENT)).setArgs(args);

            log.debug("Send a signal named {} upon the received message", signal.getEventName());
            runtimeService.createSignalEvent(signal.getEventName())
                .setVariables(variables)
                .send();
            alreadySentSignals.add(signal.getEventName());
          }
        }
      }

      // we send another signal for workflows listening to any message (without content being set)
      runtimeService.createSignalEvent(eventName)
          .setVariables(variables)
          .send();
    }
  }
}
