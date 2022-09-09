package com.symphony.bdk.workflow.engine.camunda.monitoring.repository;

import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.monitoring.repository.WorkflowInstQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowInstanceDomain;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WorkflowInstCmdaApiQueryRepository extends CamundaAbstractQueryRepository
    implements WorkflowInstQueryRepository {
  public WorkflowInstCmdaApiQueryRepository(RepositoryService repositoryService,
      HistoryService historyService, RuntimeService runtimeService,
      ObjectConverter objectConverter) {
    super(repositoryService, historyService, runtimeService, objectConverter);
  }

  /**
   * When workflow's first activity starts execution, a row is inserted in historyService's ACT_HI_PROCINST table.
   * This provides us process instances ids.
   * Tested with an activity having its first activity doing a sleep of 3minutes. During the 3 minutes,
   * while the first activity execution is ongoing, this method returns the process instance id.
   *
   * @param id
   * @return
   */
  @Override
  public List<WorkflowInstanceDomain> findAllById(String id) {
    return objectConverter.convertCollection(historyService.createHistoricProcessInstanceQuery()
        .processDefinitionKey(id).orderByProcessInstanceStartTime().asc().list(), WorkflowInstanceDomain.class);
  }
}
