package com.symphony.bdk.workflow.event;

import static java.util.Collections.singletonMap;

import com.symphony.bdk.gen.api.model.V4SymphonyElementsAction;
import com.symphony.bdk.workflow.engine.camunda.variable.FormVariableListener;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.EventHolder;
import com.symphony.bdk.workflow.engine.executor.message.SendMessageExecutor;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class V4ElementActionEventProcessor extends AbstractRealTimeEventProcessor<V4SymphonyElementsAction> {

  public V4ElementActionEventProcessor(RuntimeService runtimeService) {
    super(runtimeService, WorkflowEventType.FORM_REPLIED.getEventName());
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  protected void processEventSource(V4SymphonyElementsAction eventSource, Map<String, Object> variables)
      throws Exception {
    // we expect the activity id to be the same as the form id to work
    // correlation across processes is based on the message id that was created to send the form
    String formId = eventSource.getFormId();
    log.debug("received form reply [{}]", formId);
    Map<String, Object> formReplies = (Map<String, Object>) eventSource.getFormValues();
    variables.put(FormVariableListener.FORM_VARIABLES, singletonMap(formId, formReplies));
    ((EventHolder) variables.get(ActivityExecutorContext.EVENT)).getArgs().put(EVENT_NAME_KEY, eventName + formId);

    runtimeService.createMessageCorrelation(eventName + formId)
        .processInstanceVariableEquals(String.format("%s.%s.%s", formId, ActivityExecutorContext.OUTPUTS,
            SendMessageExecutor.OUTPUT_MESSAGE_ID_KEY), eventSource.getFormMessageId())
        .setVariables(variables)
        .correlateAll();
  }
}
