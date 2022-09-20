package com.symphony.bdk.workflow.engine.camunda.monitoring.repository;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstLifeCycleFilter;
import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.monitoring.repository.ActivityQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.domain.ActivityInstanceDomain;
import com.symphony.bdk.workflow.monitoring.repository.domain.VariablesDomain;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricActivityInstanceQuery;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
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
      ObjectConverter objectConverter) {
    super(repositoryService, historyService, runtimeService, objectConverter);
  }

  /**
   * Instance activities are the sum of executed activities from the historyService (with status COMPLETED)
   * and the activities not started yet from the workflow mapping object (with status PENDING)
   * and eventually the one activity being executed (with status ONGOING).
   */
  @Override
  public List<ActivityInstanceDomain> findAllByWorkflowInstanceId(String instanceId,
      WorkflowInstLifeCycleFilter lifeCycleFilter) {
    HistoricActivityInstanceQuery historicActivityInstanceQuery = historyService.createHistoricActivityInstanceQuery()
        .processInstanceId(instanceId);

    if (lifeCycleFilter.getStartedBefore() != null) {
      historicActivityInstanceQuery.startedBefore(new Date(lifeCycleFilter.getStartedBefore() * 1000));
    }

    if (lifeCycleFilter.getStartedAfter() != null) {
      historicActivityInstanceQuery.startedAfter(new Date(lifeCycleFilter.getStartedAfter() * 1000));
    }

    if (lifeCycleFilter.getFinishedBefore() != null) {
      historicActivityInstanceQuery.finishedBefore(new Date(lifeCycleFilter.getFinishedBefore() * 1000));
    }

    if (lifeCycleFilter.getFinishedAfter() != null) {
      historicActivityInstanceQuery.finishedAfter(new Date(lifeCycleFilter.getFinishedAfter() * 1000));
    }

    List<ActivityInstanceDomain> result = objectConverter.convertCollection(historicActivityInstanceQuery
        .orderByHistoricActivityInstanceStartTime()
        .asc()
        .list(), ActivityInstanceDomain.class);

    List<String> serviceTasks = result.stream()
        .filter(a -> a.getType().equals("serviceTask"))
        .map(ActivityInstanceDomain::getName)
        .collect(Collectors.toList());

    List<HistoricVariableInstance> historicVariableInstances =
        getHistoricVariableInstances(instanceId, serviceTasks);

    Map<String, VariablesDomain> variablesDomainMap =
        historicVariableInstances.stream().collect(Collectors.toMap(e -> e.getName(), e -> {
          Map<String, Object> objectMap = (Map<String, Object>) e.getValue();
          VariablesDomain domain = new VariablesDomain();
          domain.setOutputs((Map<String, Object>) objectMap.get("outputs"));
          domain.setUpdateTime(e.getCreateTime().toInstant());
          return domain;
        }));

    result.stream()
        .filter(a -> a.getType().equals("serviceTask"))
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
