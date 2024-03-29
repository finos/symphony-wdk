package com.symphony.bdk.workflow.engine.handler.variable;

import com.symphony.bdk.workflow.engine.WorkflowDirectedGraph;
import com.symphony.bdk.workflow.engine.camunda.WorkflowDirectedGraphService;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.EventHolder;
import com.symphony.bdk.workflow.engine.handler.HistoricEventAction;
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
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class WorkflowEventVariableAction implements HistoricEventAction {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final WorkflowDirectedGraphService workflowDirectedGraphService;
  private final RuntimeService runtimeService;

  public WorkflowEventVariableAction(WorkflowDirectedGraphService workflowDirectedGraphService,
      @Lazy RuntimeService runtimeService) {
    this.workflowDirectedGraphService = workflowDirectedGraphService;
    this.runtimeService = runtimeService;
  }

  @Override
  public void execute(HistoryEvent historyEvent) {
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

        WorkflowDirectedGraph directGraph =
            workflowDirectedGraphService.getDirectedGraph(event.getProcessDefinitionKey());
        if (eventName != null && directGraph != null) {
          String escapedEventName = RegExUtils.replaceAll((String) eventName, "[\\$\\#]", "\\\\$0");
          eventId = directGraph.readWorkflowNode(escapedEventName).getEventId();
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
