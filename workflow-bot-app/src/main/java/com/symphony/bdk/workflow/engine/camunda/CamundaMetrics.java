package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.workflow.engine.WorkflowEngineMetrics;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CamundaMetrics implements WorkflowEngineMetrics {
  private final RuntimeService runtimeService;
  private final RepositoryService repositoryService;
  private final HistoryService historyService;

  @Override
  public long countDeployedWorkflows() {
    return repositoryService.createProcessDefinitionQuery().active().count();
  }

  @Override
  public long countRunningProcesses() {
    return runtimeService.createProcessInstanceQuery().active().count();
  }

  @Override
  public long countCompletedProcesses() {
    return historyService.createHistoricProcessInstanceQuery().finished().count();
  }

  @Override
  public long countRunningActivities() {
    return runtimeService.createExecutionQuery().active().count();
  }

  @Override
  public long countCompletedActivities() {
    return historyService.createHistoricActivityInstanceQuery()
        .activityType("scriptTask").activityType("serviceTask") // exclude events
        .finished().count();

  }
}
