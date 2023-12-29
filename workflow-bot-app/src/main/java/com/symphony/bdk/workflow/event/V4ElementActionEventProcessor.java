package com.symphony.bdk.workflow.event;

import com.symphony.bdk.gen.api.model.V4SymphonyElementsAction;
import com.symphony.bdk.workflow.engine.camunda.variable.FormVariableListener;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.EventHolder;
import com.symphony.bdk.workflow.engine.executor.message.SendMessageExecutor;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.MismatchingMessageCorrelationException;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.camunda.bpm.engine.runtime.VariableInstance;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.singletonMap;

@Service
@Slf4j
public class V4ElementActionEventProcessor extends AbstractRealTimeEventProcessor<V4SymphonyElementsAction> {

  public V4ElementActionEventProcessor(RuntimeService runtimeService) {
    super(runtimeService, WorkflowEventType.FORM_REPLIED.getEventName());
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  protected void processEventSource(V4SymphonyElementsAction eventSource, Map<String, Object> variables) {
    // we expect the activity id to be the same as the form id to work
    // correlation across processes is based on the message id that was created to send the form
    String formId = eventSource.getFormId();
    log.debug("received form reply [{}]", formId);
    Map<String, Object> formReplies = (Map<String, Object>) eventSource.getFormValues();
    variables.put(FormVariableListener.FORM_VARIABLES, singletonMap(formId, formReplies));
    ((EventHolder) variables.get(ActivityExecutorContext.EVENT)).getArgs().put(EVENT_NAME_KEY, eventName + formId);

    MessageCorrelationBuilder correlationBuilder = runtimeService.createMessageCorrelation(
        eventName + formId).setVariables(variables);
    Optional<String> processId = getProcessToExecute(formId, eventSource.getFormMessageId());

    if (processId.isPresent()) {
      correlationBuilder.processInstanceId(processId.get()).correlateAll();
    } else {
      // In case the form is in the starting activity, there will be no ongoing process
      try {
        correlationBuilder.startMessageOnly().correlateAll();
      } catch (MismatchingMessageCorrelationException correlationException) {
        log.debug("This happens when no ongoing process is waiting the form {} reply event. {}",
            formId, correlationException.getMessage());
      }
    }
  }


  /**
   * This method returns the process to be executed when a form has been actioned.
   * Given 2 forms with the same formId have been sent in 2 different processes, when one of them is actioned,
   * we want to resume only the process in which context this form has been sent, hence the filter done with the formId
   * and messageId, since both forms have the same formId but different messageIds.
   *
   * @param formId    on which the action is applied.
   * @param messageId of the form.
   * @return process instance id to be resumed.
   */
  private Optional<String> getProcessToExecute(String formId, String messageId) {
    return runtimeService.createVariableInstanceQuery()
        .variableName(String.format("%s.%s.%s", formId, ActivityExecutorContext.OUTPUTS,
            SendMessageExecutor.OUTPUT_MESSAGE_IDS_KEY))
        .list()
        .stream()
        .filter(a -> ((List) a.getValue())
            .contains(messageId)) // if the workflow has many process instances, this filter could impact performance
        .map(VariableInstance::getProcessInstanceId)
        .findFirst();
  }
}
