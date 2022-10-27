package com.symphony.bdk.workflow.engine;

import com.symphony.bdk.workflow.engine.camunda.WorkflowDirectGraphCachingService;
import com.symphony.bdk.workflow.engine.camunda.WorkflowEventToCamundaEvent;
import com.symphony.bdk.workflow.engine.camunda.audit.AuditTrailLogger;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.EventHolder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.impl.history.event.HistoricVariableUpdateEventEntity;
import org.camunda.bpm.engine.impl.history.event.HistoryEvent;
import org.camunda.bpm.engine.impl.history.handler.HistoryEventHandler;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class WorkflowEventHandler implements HistoryEventHandler {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final WorkflowDirectGraphCachingService workflowDirectGraphCachingService;
  private final AuditTrailLogger auditTrailLogger;
  private final RuntimeService runtimeService;

  public WorkflowEventHandler(@Lazy RuntimeService runtimeService, @Lazy AuditTrailLogger auditTrailLogger,
      WorkflowDirectGraphCachingService workflowDirectGraphCachingService) {
    this.auditTrailLogger = auditTrailLogger;
    this.runtimeService = runtimeService;
    this.workflowDirectGraphCachingService = workflowDirectGraphCachingService;
  }

  @Override
  public void handleEvent(HistoryEvent historyEvent) {
    this.auditTrailLogger.handleEvent(historyEvent);

    if (historyEvent instanceof HistoricVariableUpdateEventEntity) {
      this.storeEventHolderVariable((HistoricVariableUpdateEventEntity) historyEvent);
    }
  }

  @Override
  public void handleEvents(List<HistoryEvent> historyEvents) {
    historyEvents.forEach(this::handleEvent);
  }

  /**
   * This method stores the event holder in the runtime service variables in order to be accessible by SWADL.
   * The incoming event name is {@link ActivityExecutorContext#EVENT} but the new event name is passed in {@link EventHolder#getArgs()}.
   *
   * @param event: event updating the variables.
   */
  private void storeEventHolderVariable(HistoricVariableUpdateEventEntity event) {
    if (ActivityExecutorContext.EVENT.equals(event.getVariableName()) && event.getByteValue() != null) {
      try {
        EventHolder eventHolder =
            OBJECT_MAPPER.readValue(new String(event.getByteValue(), StandardCharsets.UTF_8), EventHolder.class);

        Object eventName = eventHolder.getArgs().get(WorkflowEventToCamundaEvent.EVENT_NAME);
        String eventId = workflowDirectGraphCachingService.getDirectGraph(event.getProcessDefinitionKey())
            .readWorkflowNode((String) eventName)
            .getEventId();

        if (eventId != null) {
          // remove event name from args
          eventHolder.getArgs().remove(WorkflowEventToCamundaEvent.EVENT_NAME);

          // store the event with the new key
          this.runtimeService.setVariable(event.getExecutionId(), eventId, eventHolder);
        }
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    }
  }

}
