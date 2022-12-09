package com.symphony.bdk.workflow.engine.camunda.monitoring.repository;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstLifeCycleFilter;
import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.monitoring.repository.ActivityQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.domain.ActivityInstanceDomain;
import com.symphony.bdk.workflow.monitoring.repository.domain.VariablesDomain;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricActivityInstanceQuery;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.management.JobDefinition;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.EventSubscription;
import org.camunda.bpm.engine.runtime.Execution;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ActivityCmdaApiQueryRepository extends CamundaAbstractQueryRepository implements ActivityQueryRepository {

  public ActivityCmdaApiQueryRepository(RepositoryService repositoryService,
      HistoryService historyService, RuntimeService runtimeService,
      ObjectConverter objectConverter, ManagementService managementService) {
    super(repositoryService, historyService, runtimeService, managementService, objectConverter);
  }

  @Override
  public void test(String processDefinitionVersion, String startingEventName) {

    List<ProcessDefinition> list =
        repositoryService.createProcessDefinitionQuery().versionTag(processDefinitionVersion).list();
    String processDefinitionId = list.get(0).getId();
    List<JobDefinition> jobDefinitions =
        this.managementService.createJobDefinitionQuery().processDefinitionId(processDefinitionId).list();

    List<JobDefinition> startEvent =
        jobDefinitions.stream().filter(jobDefinition -> jobDefinition.getActivityId().startsWith("startEvent")).collect(
            Collectors.toList());

    String startEventTaskId = startEvent.get(0).getActivityId();

    List<EventSubscription> subscriptionList =
        this.runtimeService.createEventSubscriptionQuery().eventName(startingEventName).list();

    String subscriptionId = subscriptionList.get(0).getId();

    String query =
        String.format("UPDATE ACT_RU_EVENT_SUBSCR SET CONFIGURATION_ = '%s',ACTIVITY_ID_ = '%s' where ID_ = '%s'",
            processDefinitionId, startEventTaskId, subscriptionId);
    List<Execution> queryResult = runtimeService.createNativeExecutionQuery().sql(query).list();

    System.out.println(queryResult);
  }

  /**
   * Instance activities are the sum of executed activities from the historyService (with status COMPLETED)
   * and the activities not started yet from the workflow mapping object (with status PENDING)
   * and eventually the one activity being executed (with status ONGOING).
   */
  @Override
  public List<ActivityInstanceDomain> findAllByWorkflowInstanceId(String workflowId, String instanceId,
      WorkflowInstLifeCycleFilter lifeCycleFilter) {
    HistoricActivityInstanceQuery historicActivityInstanceQuery = historyService.createHistoricActivityInstanceQuery()
        .processInstanceId(instanceId);

    if (lifeCycleFilter.getStartedBefore() != null) {
      historicActivityInstanceQuery.startedBefore(Date.from(lifeCycleFilter.getStartedBefore()));
    }

    if (lifeCycleFilter.getStartedAfter() != null) {
      historicActivityInstanceQuery.startedAfter(Date.from(lifeCycleFilter.getStartedAfter()));
    }

    if (lifeCycleFilter.getFinishedBefore() != null) {
      historicActivityInstanceQuery.finishedBefore(Date.from(lifeCycleFilter.getFinishedBefore()));
    }

    if (lifeCycleFilter.getFinishedAfter() != null) {
      historicActivityInstanceQuery.finishedAfter(Date.from(lifeCycleFilter.getFinishedAfter()));
    }

    List<ActivityInstanceDomain> result = objectConverter.convertCollection(historicActivityInstanceQuery
        .orderByHistoricActivityInstanceStartTime()
        .asc()
        .list(), ActivityInstanceDomain.class);

    List<HistoricVariableInstance> historicVariableInstances = getHistoricVariableInstances(instanceId,
        result.stream()
            .filter(a -> a.getType().equals("serviceTask"))
            .map(ActivityInstanceDomain::getName)
            .collect(Collectors.toList()));

    Map<String, VariablesDomain> variablesDomainMap =
        historicVariableInstances.stream().collect(Collectors.toMap(HistoricVariableInstance::getName, e -> {
          Map<String, Object> objectMap = (Map<String, Object>) e.getValue();
          VariablesDomain domain = new VariablesDomain();
          domain.setOutputs((Map<String, Object>) objectMap.get("outputs"));
          domain.setUpdateTime(e.getCreateTime().toInstant());
          return domain;
        }));

    result.stream()
        .filter(a -> a.getType().equals("serviceTask") && variablesDomainMap.get(a.getName()) != null)
        .forEach(activity -> activity.setVariables(variablesDomainMap.get(activity.getName())));

    return result;
  }

  private List<HistoricVariableInstance> getHistoricVariableInstances(String instanceId, List<String> serviceTasks) {
    if (!serviceTasks.isEmpty()) {
      StringBuilder sql = new StringBuilder("select * from ACT_HI_VARINST where PROC_INST_ID_ = '");
      sql.append(instanceId).append("'");

      sql.append(" and NAME_ in (");
      for (String name : serviceTasks) {
        sql.append("'").append(name).append("',");
      }
      sql.deleteCharAt(sql.length() - 1);
      sql.append(")");

      return historyService.createNativeHistoricVariableInstanceQuery().sql(sql.toString()).list();
    }
    return Collections.emptyList();
  }
}
