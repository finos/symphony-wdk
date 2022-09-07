package com.symphony.bdk.workflow.engine.camunda.monitor.repository;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class WorkflowInstanceMonitoringJavaApiRepository
    implements WorkflowInstanceMonitoringRepository<HistoricProcessInstance> {
  private final HistoryService historyService;

  @Override
  public List<HistoricProcessInstance> listWorkflowInstances(String workflowId) {
    /*
      When workflow's first activity starts execution, a row is inserted in historyService's ACT_HI_PROCINST table.
      This provides us process instances ids.
      Tested with an activity having its first activity doing a sleep of 3minutes. During the 3 minutes,
      while the first activity execution is ongoing, this method returns the process instance id.
     */
    return historyService.createHistoricProcessInstanceQuery()
        .processDefinitionKey(workflowId)
        .list();
  }

}
