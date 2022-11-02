package com.symphony.bdk.workflow.engine.camunda.audit;

import com.symphony.bdk.workflow.engine.EventHandler;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.impl.history.event.HistoricActivityInstanceEventEntity;
import org.camunda.bpm.engine.impl.history.event.HistoricJobLogEvent;
import org.camunda.bpm.engine.impl.history.event.HistoricProcessInstanceEventEntity;
import org.camunda.bpm.engine.impl.history.event.HistoricVariableUpdateEventEntity;
import org.camunda.bpm.engine.impl.history.event.HistoryEvent;
import org.camunda.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.repository.Deployment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Log Camunda events in a specific topic/logger. Events are logged using a key1=value1, key2=value2 format.
 */
@Slf4j(topic = "audit-trail")
@Component
public class AuditTrailLogger implements EventHandler {

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

  private void logJobEvent(HistoricJobLogEvent event) {
    log.info("job={}, job_type={}, process={}, process_key={}, activity={}",
        event.getJobId(), event.getJobDefinitionType(),
        event.getProcessInstanceId(), event.getProcessDefinitionKey(),
        event.getActivityId());
  }

  private void logProcessEvent(HistoricProcessInstanceEventEntity event) {
    if (event.getDurationInMillis() == null) {
      log.info("event={}_process, process={}, process_key={}",
          event.getEventType(),
          event.getProcessInstanceId(), event.getProcessDefinitionKey());
    } else {
      log.info("event={}_process, process={}, process_key={}, duration={}",
          event.getEventType(),
          event.getProcessInstanceId(), event.getProcessDefinitionKey(),
          event.getDurationInMillis());
    }
  }

  private void logActivityEvent(HistoricActivityInstanceEventEntity event) {
    if (event.getDurationInMillis() == null) {
      log.info("event={}_activity, process={}, process_key={}, activity={}, activity_name={}",
          event.getEventType(),
          event.getProcessInstanceId(), event.getProcessDefinitionKey(),
          event.getActivityId(), event.getActivityName());
    } else {
      log.info("event={}_activity, process={}, process_key={}, activity={}, activity_name={}, duration={}",
          event.getEventType(),
          event.getProcessInstanceId(), event.getProcessDefinitionKey(),
          event.getActivityId(), event.getActivityName(),
          event.getDurationInMillis());
    }
  }

  private void logVariableEvent(HistoricVariableUpdateEventEntity event) {
    // for DF events the initiator variable is set to pass the user id that triggered the execution
    if (ActivityExecutorContext.INITIATOR.equals(event.getVariableName())
        && event.getLongValue() != null) {
      log.info("initiator={}, process={}, process_key={}",
          event.getLongValue(),
          event.getProcessInstanceId(), event.getProcessDefinitionKey());
    }
  }

  public void deployed(Deployment deployment) {
    log.info("event={}, deployment={}, deployment_name={}, process_key={}",
        "deploy_workflow", deployment.getId(), deployment.getName(), getProcessKey(deployment));
  }

  public void undeployed(Deployment deployment) {
    log.info("event={}, deployment={}, deployment_name={}",
        "undeploy_workflow", deployment.getId(), deployment.getName());
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

  public void execute(DelegateExecution execution, String activityType) {
    log.info("event={}, process={}, process_key={}, activity={}, activity_name={}, activity_type={}",
        "execute_activity", execution.getProcessDefinitionId(),
        ((ExecutionEntity) execution).getProcessDefinition().getKey(),
        execution.getCurrentActivityId(), execution.getCurrentActivityName(),
        activityType);
  }
}
