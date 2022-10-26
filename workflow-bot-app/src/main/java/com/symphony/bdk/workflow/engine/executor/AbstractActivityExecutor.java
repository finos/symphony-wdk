package com.symphony.bdk.workflow.engine.executor;

import com.symphony.bdk.core.service.message.exception.PresentationMLParserException;
import com.symphony.bdk.core.service.message.util.PresentationMLParser;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.gen.api.model.V4MessageSuppressed;
import com.symphony.bdk.gen.api.model.V4SharedPost;
import com.symphony.bdk.gen.api.model.V4SymphonyElementsAction;
import com.symphony.bdk.workflow.engine.camunda.audit.AuditTrailLogger;
import com.symphony.bdk.workflow.swadl.v1.EventResolver;
import com.symphony.bdk.workflow.swadl.v1.EventWithTimeout;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;
import com.symphony.bdk.workflow.swadl.v1.event.FormRepliedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.MessageReceivedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.MessageSuppressedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.PostSharedEvent;

import org.camunda.bpm.engine.RuntimeService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractActivityExecutor<T extends BaseActivity> {

  private static final String EVENT_VARIABLE_PREFIX = "event_";
  private final RuntimeService runtimeService;

  protected AbstractActivityExecutor(RuntimeService runtimeService) {
    this.runtimeService = runtimeService;
  }

  private List<String> retrieveKeysOfType(Map<String, Object> map, String type) {
    return map.keySet().stream().filter(key -> key.contains(type)).collect(Collectors.toList());
  }

  protected void setEventVariables(ActivityExecutorContext<T> execution) {
    EventWithTimeout onEvents = execution.getActivity().getOn();

    if (onEvents == null) {
      return;
    }

    EventResolver eventResolver = new EventResolver(onEvents);

    Map<String, Object> tempVariables = runtimeService.getVariables(execution.getProcessInstanceId())
        .entrySet()
        .stream()
        .filter(entry -> entry.getKey().startsWith(AuditTrailLogger.TEMPORARY_EVENT_KEY))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    if (eventResolver.isEventOfType(MessageReceivedEvent.class.getSimpleName())) {
      List<Object> eventsOfType = eventResolver.getAllEventsOfType(MessageReceivedEvent.class.getSimpleName());
      List<String> keys = retrieveKeysOfType(tempVariables, V4MessageSent.class.getSimpleName());

      keys.forEach(key -> {
        V4MessageSent v4MessageSent = ((EventHolder<V4MessageSent>) execution.getVariables().get(key)).getSource();
        eventsOfType.stream()
            .filter(e -> {
              try {
                return PresentationMLParser.getTextContent(v4MessageSent.getMessage().getMessage())
                    .equals(((MessageReceivedEvent) e).getContent());
              } catch (PresentationMLParserException ex) {
                return false;
              }
            })
            .findAny()
            .ifPresent(o -> runtimeService.setVariable(execution.getProcessInstanceId(),
                EVENT_VARIABLE_PREFIX + ((MessageReceivedEvent) o).getId(),
                runtimeService.getVariable(execution.getProcessInstanceId(), key)));
      });

//      runtimeService.removeVariables(execution.getProcessInstanceId(), keys);
    }

    if (eventResolver.isEventOfType(FormRepliedEvent.class.getSimpleName())) {
      List<Object> eventsOfType = eventResolver.getAllEventsOfType(FormRepliedEvent.class.getSimpleName());
      List<String> keys = retrieveKeysOfType(tempVariables, V4SymphonyElementsAction.class.getSimpleName());

      keys.forEach(key -> {
        V4SymphonyElementsAction v4SymphonyElementsAction =
            ((EventHolder<V4SymphonyElementsAction>) execution.getVariables().get(key)).getSource();
        eventsOfType.stream()
            .filter(e -> v4SymphonyElementsAction.getFormId() != null && v4SymphonyElementsAction.getFormId()
                .equals(((FormRepliedEvent) e).getFormId()))
            .findAny()
            .ifPresent(
                o -> runtimeService.setVariable(execution.getProcessInstanceId(),
                    EVENT_VARIABLE_PREFIX + ((FormRepliedEvent) o).getId(),
                    runtimeService.getVariable(execution.getProcessInstanceId(), key)));
      });
    }

    if (eventResolver.isEventOfType(MessageSuppressedEvent.class.getSimpleName())) {
      List<Object> eventsOfType = eventResolver.getAllEventsOfType(MessageSuppressedEvent.class.getSimpleName());
      List<String> keys = retrieveKeysOfType(tempVariables, V4MessageSuppressed.class.getSimpleName());

      var ref = new Object() {
        V4MessageSuppressed v4MessageSuppressed = null;
      };

      if (!keys.isEmpty()) {
        ref.v4MessageSuppressed =
            ((EventHolder<V4MessageSuppressed>) runtimeService.getVariable(execution.getProcessInstanceId(), keys.get(0))).getSource();
      }

      eventsOfType.forEach(e -> runtimeService.setVariable(execution.getProcessInstanceId(),
          EVENT_VARIABLE_PREFIX + ((MessageSuppressedEvent) e).getId(), ref.v4MessageSuppressed));
    }

    if (eventResolver.isEventOfType(PostSharedEvent.class.getSimpleName())) {
      List<Object> eventsOfType = eventResolver.getAllEventsOfType(PostSharedEvent.class.getSimpleName());
      List<String> keys = retrieveKeysOfType(tempVariables, V4SharedPost.class.getSimpleName());

      /*eventsOfType.forEach(e -> runtimeService.setVariable(execution.getProcessInstanceId(),
          EVENT_VARIABLE_PREFIX + ((PostSharedEvent) e).getId(),
          runtimeService.getVariable(execution.getProcessInstanceId(), key)))*/
    }

    // remove temporary variables
    runtimeService.removeVariables(execution.getProcessInstanceId(), tempVariables.keySet());
  }

  private void doIt(Class<T> eventInternalType, Class<T> eventExternalType, EventResolver eventResolver, Map<String, Object> tempVariables) {
    List<Object> eventsOfType = eventResolver.getAllEventsOfType(eventExternalType.getSimpleName());
    List<String> keys = retrieveKeysOfType(tempVariables, eventExternalType.getSimpleName());

   // keys.forEach(key -> even);
  }
}
