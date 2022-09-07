package com.symphony.bdk.workflow.engine.camunda.monitor.repository;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class WorkflowMonitoringJavaApiRepository implements WorkflowMonitoringRepository<ProcessDefinition> {
  private final RepositoryService repositoryService;

  @Override
  public List<ProcessDefinition> listAllWorkflows() {
    return repositoryService.createProcessDefinitionQuery().list();
  }
}
