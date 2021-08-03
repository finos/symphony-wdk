package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.impl.history.event.HistoricActivityInstanceEventEntity;
import org.camunda.bpm.engine.impl.history.event.HistoricJobLogEvent;
import org.camunda.bpm.engine.impl.history.event.HistoricProcessInstanceEventEntity;
import org.camunda.bpm.engine.impl.history.event.HistoricVariableUpdateEventEntity;
import org.camunda.bpm.engine.impl.history.event.HistoryEvent;
import org.camunda.bpm.engine.impl.history.handler.HistoryEventHandler;

import java.util.List;

/**
 * Log Camunda events in a specific topic/logger. Events are logged using a key1=value1, key2=value2 format.
 */
@Slf4j(topic = "audit-trail")
public class HistoryEventLogger implements HistoryEventHandler {

  @Override
  public void handleEvent(HistoryEvent historyEvent) {
    if (historyEvent instanceof HistoricJobLogEvent) {
      HistoricJobLogEvent jobLogEvent = (HistoricJobLogEvent) historyEvent;
      log.info("job={}, job_type={}, process={}, process_key={}, activity={}",
          jobLogEvent.getJobId(), jobLogEvent.getJobDefinitionType(),
          jobLogEvent.getProcessDefinitionId(), jobLogEvent.getProcessDefinitionKey(),
          jobLogEvent.getActivityId());

    } else if (historyEvent instanceof HistoricProcessInstanceEventEntity) {
      HistoricProcessInstanceEventEntity instanceEvent = (HistoricProcessInstanceEventEntity) historyEvent;
      if (instanceEvent.getDurationInMillis() == null) {
        log.info("event={}, process={}, process_key={}",
            instanceEvent.getEventType(),
            instanceEvent.getProcessDefinitionId(), instanceEvent.getProcessDefinitionKey());
      } else {
        log.info("event={}, process={}, process_key={}, duration={}",
            instanceEvent.getEventType(),
            instanceEvent.getProcessDefinitionId(), instanceEvent.getProcessDefinitionKey(),
            instanceEvent.getDurationInMillis());
      }

    } else if (historyEvent instanceof HistoricActivityInstanceEventEntity) {
      HistoricActivityInstanceEventEntity instanceEvent = (HistoricActivityInstanceEventEntity) historyEvent;
      if (instanceEvent.getDurationInMillis() == null) {
        log.info("event={}, process={}, process_key={}, activity={}, activity_name={}",
            instanceEvent.getEventType(),
            instanceEvent.getProcessDefinitionId(), instanceEvent.getProcessDefinitionKey(),
            instanceEvent.getActivityId(), instanceEvent.getActivityName());
      } else {
        log.info("event={}, process={}, process_key={}, activity={}, activity_name={}, duration={}",
            instanceEvent.getEventType(),
            instanceEvent.getProcessDefinitionId(), instanceEvent.getProcessDefinitionKey(),
            instanceEvent.getActivityId(), instanceEvent.getActivityName(),
            instanceEvent.getDurationInMillis());
      }

    } else if (historyEvent instanceof HistoricVariableUpdateEventEntity) {
      HistoricVariableUpdateEventEntity variableEvent = (HistoricVariableUpdateEventEntity) historyEvent;

      // for DF2 events the initiator variable is set to pass the user id that triggered the execution
      if (ActivityExecutorContext.INITIATOR.equals(variableEvent.getVariableName())
          && variableEvent.getLongValue() != null) {
        log.info("initiator={}, process={}, process_key={}",
            variableEvent.getLongValue(),
            variableEvent.getProcessDefinitionId(), variableEvent.getProcessDefinitionKey());
      }

    } else {
      log.trace("Event {}", historyEvent);
    }
  }

  @Override
  public void handleEvents(List<HistoryEvent> historyEvents) {
    for (HistoryEvent historyEvent : historyEvents) {
      handleEvent(historyEvent);
    }
  }
}
