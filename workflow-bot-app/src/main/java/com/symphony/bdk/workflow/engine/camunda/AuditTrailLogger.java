package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.impl.history.event.HistoricActivityInstanceEventEntity;
import org.camunda.bpm.engine.impl.history.event.HistoricJobLogEvent;
import org.camunda.bpm.engine.impl.history.event.HistoricProcessInstanceEventEntity;
import org.camunda.bpm.engine.impl.history.event.HistoricVariableUpdateEventEntity;
import org.camunda.bpm.engine.impl.history.event.HistoryEvent;
import org.camunda.bpm.engine.impl.history.handler.HistoryEventHandler;
import org.camunda.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.repository.Deployment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Log Camunda events in a specific topic/logger. Events are logged using a key1=value1, key2=value2 format.
 */
@Slf4j(topic = "audit-trail")
@Component
public class AuditTrailLogger implements HistoryEventHandler {

  @Override
  public void handleEvent(HistoryEvent historyEvent) {
    if (historyEvent instanceof HistoricJobLogEvent) {
      logJobEvent((HistoricJobLogEvent) historyEvent);

    } else if (historyEvent instanceof HistoricProcessInstanceEventEntity) {
      logProcessEvent((HistoricProcessInstanceEventEntity) historyEvent);

    } else if (historyEvent instanceof HistoricActivityInstanceEventEntity) {
      logActivityEvent((HistoricActivityInstanceEventEntity) historyEvent);

    } else if (historyEvent instanceof HistoricVariableUpdateEventEntity) {
      logVariableEvent((HistoricVariableUpdateEventEntity) historyEvent);

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

  private void logJobEvent(HistoricJobLogEvent event) {
    log.info("event={}, job={}, job_type={}, process={}, process_key={}, activity={}",
        event.getEventType(), event.getJobId(), event.getJobDefinitionType(),
        event.getProcessDefinitionId(), event.getProcessDefinitionKey(),
        event.getActivityId());
  }

  private void logProcessEvent(HistoricProcessInstanceEventEntity event) {
    if (event.getDurationInMillis() == null) {
      log.info("event={}_process, process={}, process_key={}",
          event.getEventType(),
          event.getProcessDefinitionId(), event.getProcessDefinitionKey());
    } else {
      log.info("event={}_process, process={}, process_key={}, duration={}",
          event.getEventType(),
          event.getProcessDefinitionId(), event.getProcessDefinitionKey(),
          event.getDurationInMillis());
    }
  }

  private void logActivityEvent(HistoricActivityInstanceEventEntity event) {
    if (event.getDurationInMillis() == null) {
      log.info("event={}_activity, process={}, process_key={}, activity={}, activity_name={}",
          event.getEventType(),
          event.getProcessDefinitionId(), event.getProcessDefinitionKey(),
          event.getActivityId(), event.getActivityName());
    } else {
      log.info("event={}_activity, process={}, process_key={}, activity={}, activity_name={}, duration={}",
          event.getEventType(),
          event.getProcessDefinitionId(), event.getProcessDefinitionKey(),
          event.getActivityId(), event.getActivityName(),
          event.getDurationInMillis());
    }
  }

  private void logVariableEvent(HistoricVariableUpdateEventEntity event) {
    // for DF2 events the initiator variable is set to pass the user id that triggered the execution
    if (ActivityExecutorContext.INITIATOR.equals(event.getVariableName())
        && event.getLongValue() != null) {
      log.info("initiator={}, process={}, process_key={}",
          event.getLongValue(),
          event.getProcessDefinitionId(), event.getProcessDefinitionKey());
    }
  }

  public void deployed(Deployment deployment) {
    log.info("event={}, deployment={}, deployment_name={}, process_key={}",
        "deploy", deployment.getId(), deployment.getName(), getProcessKey(deployment));
  }

  public void undeployed(Deployment deployment) {
    log.info("event={}, deployment={}, deployment_name={}, process_key={}",
        "undeploy", deployment.getId(), deployment.getName(), getProcessKey(deployment));
  }

  @SuppressWarnings("rawtypes")
  private String getProcessKey(Deployment deployment) {
    if (deployment instanceof DeploymentEntity) {
      List artifacts = ((DeploymentEntity) deployment).getDeployedArtifacts().get(ProcessDefinitionEntity.class);
      if (artifacts != null
          && !artifacts.isEmpty()
          && artifacts.get(0) instanceof ProcessDefinitionEntity) {
        ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) artifacts.get(0);
        return processDefinition.getKey();
      }
    }
    return "";
  }
}
