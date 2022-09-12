package com.symphony.bdk.workflow.engine.camunda.monitoring.repository;

import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.monitoring.repository.WorkflowQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowDomain;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class WorkflowCmdaApiQueryRepository extends CamundaAbstractQueryRepository implements WorkflowQueryRepository {

  public WorkflowCmdaApiQueryRepository(RepositoryService repositoryService,
      HistoryService historyService, RuntimeService runtimeService,
      ObjectConverter objectConverter) {
    super(repositoryService, historyService, runtimeService, objectConverter);
  }

  @Override
  public List<WorkflowDomain> findAll() {
    return objectConverter.convertCollection(repositoryService.createProcessDefinitionQuery().list(),
        WorkflowDomain.class);
  }
}
