package com.symphony.bdk.workflow.event;

import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.EventHolder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public abstract class AbstractRealTimeEventProcessor<T> implements RealTimeEventProcessor<T> {

  protected final RuntimeService runtimeService;
  protected final String eventName;

  @Override
  public void process(RealTimeEvent<T> event) throws Exception {
    Map<String, Object> processVariables = new HashMap<>();
    processVariables.put(ActivityExecutorContext.EVENT,
            new EventHolder<>(event.getInitiator(), event.getSource(), new HashMap<>()));

    if (event.getInitiator() != null
            && event.getInitiator().getUser() != null
            && event.getInitiator().getUser().getUserId() != null) {
      Long userId = event.getInitiator().getUser().getUserId();
      processVariables.put(ActivityExecutorContext.INITIATOR, userId);
      log.debug("Dispatching event {} from user {}", event.getSource().getClass().getSimpleName(), userId);
    }
    processEventSource(event.getSource(), processVariables);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  protected void processEventSource(T eventSource, Map<String, Object> variables) throws Exception {
    ((EventHolder) variables.get(ActivityExecutorContext.EVENT)).getArgs().put(EVENT_NAME_KEY, eventName);
    runtimeService.createSignalEvent(eventName).setVariables(variables).send();
  }
}
