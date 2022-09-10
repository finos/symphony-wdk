package com.symphony.bdk.workflow.engine.camunda.monitoring.repository;

import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.monitoring.repository.ActivityQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.domain.ActivityInstanceDomain;
import com.symphony.bdk.workflow.monitoring.repository.domain.VariablesDomain;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.springframework.stereotype.Component;

import java.util.Collections;
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
   * <p>
   * In order to have an ongoing activity, I tried with an execute-script having a sleep,
   * as long as the activity (the sleep) is not completed,
   * historyService and runtimeService are not updated with new rows.
   * <p>
   * --> Maybe Camunda is synchronous and needs to be completely free (no ongoing work/activity) to update database.
   * <p>
   * Let's try with a new activity who does a lot of work (a big for loop..): Same behavior
   *
   * @param instanceId
   * @return
   */
  @Override
  public List<ActivityInstanceDomain> findAllByWorkflowInstanceId(String instanceId) {
    List<ActivityInstanceDomain> result =
        objectConverter.convertCollection(historyService.createHistoricActivityInstanceQuery()
            .processInstanceId(instanceId).orderByHistoricActivityInstanceStartTime().asc()
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
