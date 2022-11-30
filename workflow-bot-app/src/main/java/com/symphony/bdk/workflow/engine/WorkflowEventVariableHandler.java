package com.symphony.bdk.workflow.engine;

import com.symphony.bdk.workflow.engine.camunda.WorkflowDirectGraphCachingService;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.EventHolder;
import com.symphony.bdk.workflow.event.RealTimeEventProcessor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.impl.history.event.HistoricVariableUpdateEventEntity;
import org.camunda.bpm.engine.impl.history.event.HistoryEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Handles workflow events to persist them in database.
 */
@Component
@Primary
@Slf4j
public class WorkflowEventVariableHandler implements EventHandler {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final WorkflowDirectGraphCachingService workflowDirectGraphCachingService;
  private final RuntimeService runtimeService;

  public WorkflowEventVariableHandler(@Lazy RuntimeService runtimeService,
      WorkflowDirectGraphCachingService workflowDirectGraphCachingService) {
    this.runtimeService = runtimeService;
    this.workflowDirectGraphCachingService = workflowDirectGraphCachingService;
  }

  @Override
  public void handleEvent(HistoryEvent historyEvent) {
    if (historyEvent instanceof HistoricVariableUpdateEventEntity) {
      this.storeEventHolderVariable((HistoricVariableUpdateEventEntity) historyEvent);
    }
  }

  /**
   * This method stores the event holder in the runtime service variables in order to be accessible by SWADL.
   * The incoming event name is {@link ActivityExecutorContext#EVENT} but the new event name is passed in
   * {@link EventHolder#getArgs()}.
   *
   * @param event event updating the variables.
   */
  private void storeEventHolderVariable(HistoricVariableUpdateEventEntity event) {
    if (ActivityExecutorContext.EVENT.equals(event.getVariableName()) && event.getByteValue() != null) {
      try {
        EventHolder eventHolder =
            OBJECT_MAPPER.readValue(new String(event.getByteValue(), StandardCharsets.UTF_8), EventHolder.class);

        Object eventName = eventHolder.getArgs().get(RealTimeEventProcessor.EVENT_NAME_KEY);
        String eventId = "";

        if (eventName != null) {
          String escapedEventName = RegExUtils.replaceAll((String) eventName, "[\\$\\#]", "\\\\$0");
          eventId = workflowDirectGraphCachingService.getDirectGraph(event.getProcessDefinitionKey())
              .readWorkflowNode(escapedEventName)
              .getEventId();
        }

        if (StringUtils.isNotBlank(eventId)) {
          // remove event name from args
          eventHolder.getArgs().remove(RealTimeEventProcessor.EVENT_NAME_KEY);

          // store the event with the new key
          this.runtimeService.setVariable(event.getExecutionId(), eventId, eventHolder);
        }
      } catch (JsonProcessingException e) {
        log.error("Failed to store event in variable {}", event.getVariableName(), e);
      }
    }
  }

}
