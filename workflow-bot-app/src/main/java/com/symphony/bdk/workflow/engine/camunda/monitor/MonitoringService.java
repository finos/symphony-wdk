package com.symphony.bdk.workflow.engine.camunda.monitor;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class MonitoringService {
  private final RepositoryService repositoryService;
  //private final RuntimeService runtimeService;
  private final HistoryService historyService;

  public List<ProcessDefinition> listAllWorkflows() {
    return repositoryService.createProcessDefinitionQuery().list();
  }

  public List<HistoricProcessInstance> listWorkflowInstances(String workflowId) {
    /*
    // APPROACH 1
    List<ProcessDefinition> processDefinitionQuery = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey(workflowId)
        .list();
    String processDefinitionId = processDefinitionQuery.get(0).getId();


    String listActiveInstancesQuery = String.format("SELECT DISTINCT (PROC_INST_ID_) "
        + "FROM ACT_RU_EXECUTION "
        + "WHERE PROC_DEF_ID_ = '%s'", processDefinitionId);
    List<String> pendingProcessInstances = runtimeService.createNativeExecutionQuery()
        .sql(listActiveInstancesQuery)
        .list()
        .stream()
        .map(Execution::getProcessInstanceId)
        .collect(Collectors.toList());


    List<String> completedProcessInstances = historyService.createHistoricProcessInstanceQuery()
        .processDefinitionKey(workflowId)
        .list()
        .stream()
        .map(HistoricProcessInstance::getId)
        .filter(id -> !pendingProcessInstances.contains(id))
        .collect(Collectors.toList());

        // TODO: add data about start/end date
    // TODO: Create corresponding pojo and mapper
    System.out.println(
        String.format("PENDING : [%s]\nCOMPLETED : [%s]", pendingProcessInstances, completedProcessInstances));
    return Collections.emptyList();
    */

    return historyService.createHistoricProcessInstanceQuery()
        .processDefinitionKey(workflowId)
        .list();
  }

  public List<HistoricActivityInstance> listInstanceActivities(String instanceId) {
    List<HistoricActivityInstance> executedActivities = this.listInstanceActivitiesFromHistoricRepository(instanceId);

    /*TODO: ADD not executed activities to the list

    List<String> executedActivityIds = executedActivities.stream()
        .map(HistoricActivityInstance::getActivityName)
        .collect(Collectors.toList());
    Workflow workflow = new Workflow();
    List<Activity> notExecutedActivities = workflow.getActivities()
        .stream()
        .filter(Predicate.not(executedActivityIds::contains))
        .collect(Collectors.toList());

    // return executedActivities and notExecutedActivities in the same list*/

    //TODO: ADD variables

    return executedActivities;
  }

  private List<HistoricActivityInstance> listInstanceActivitiesFromHistoricRepository(String instanceId) {
    return historyService.createHistoricActivityInstanceQuery()
        .processInstanceId(instanceId)
        .list()
        .stream()
        .filter(a -> a.getActivityType().equals("serviceTask") || a.getActivityType().equals("scriptTask"))
        .collect(Collectors.toList());
  }

  public void listInstanceActivitiesFromRuntimeRepository(String instanceId) {

  }
}
