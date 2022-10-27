package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.workflow.engine.camunda.audit.AuditTrailLogger;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.EventHolder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomUtils;
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

  public static final String TEMPORARY_EVENT_KEY = "tempevent_";
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
      this.storeTemporaryEventVariable((HistoricVariableUpdateEventEntity) historyEvent);
    }
  }

  @Override
  public void handleEvents(List<HistoryEvent> historyEvents) {
    historyEvents.forEach(this::handleEvent);
  }

  /**
   * This method temporarily stores the event in the runtime service variables in order to be recycled by activities executors.
   *
   * @param event: event updating the variables.
   */
  private void storeTemporaryEventVariable(HistoricVariableUpdateEventEntity event) {
    if (ActivityExecutorContext.EVENT.equals(event.getVariableName())
        && event.getByteValue() != null) {
      try {
        EventHolder eventHolder =
            OBJECT_MAPPER.readValue(new String(event.getByteValue(), StandardCharsets.UTF_8), EventHolder.class);

        this.runtimeService.setVariable(event.getExecutionId(),
            TEMPORARY_EVENT_KEY + eventHolder.getSource().getClass().getSimpleName() + "_" + RandomUtils.nextInt(),
            eventHolder);
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    }
  }

}
