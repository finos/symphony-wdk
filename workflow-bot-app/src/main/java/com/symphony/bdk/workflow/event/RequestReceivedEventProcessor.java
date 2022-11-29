package com.symphony.bdk.workflow.event;

import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.EventHolder;
import com.symphony.bdk.workflow.swadl.v1.event.RequestReceivedEvent;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class RequestReceivedEventProcessor extends AbstractRealTimeEventProcessor<RequestReceivedEvent> {

  public RequestReceivedEventProcessor(RuntimeService runtimeService) {
    super(runtimeService, WorkflowEventType.REQUEST_RECEIVED.getEventName());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  protected void processEventSource(RequestReceivedEvent eventSource, Map<String, Object> variables) throws Exception {
    String finaName = eventName + eventSource.getWorkflowId();
    Map<String, Object> args = new HashMap<>(Optional.ofNullable(eventSource.getArguments()).orElseGet(HashMap::new));
    args.put(EVENT_NAME_KEY, finaName);
    ((EventHolder) variables.get(ActivityExecutorContext.EVENT)).setArgs(args);
    runtimeService.createSignalEvent(finaName).setVariables(variables).send();
  }
}
