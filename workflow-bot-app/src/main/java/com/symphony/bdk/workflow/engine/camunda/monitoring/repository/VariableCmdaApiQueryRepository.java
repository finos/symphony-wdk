package com.symphony.bdk.workflow.engine.camunda.monitoring.repository;

import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.monitoring.repository.VariableQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.domain.VariablesDomain;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class VariableCmdaApiQueryRepository extends CamundaAbstractQueryRepository implements VariableQueryRepository {
  public VariableCmdaApiQueryRepository(RepositoryService repositoryService,
      HistoryService historyService, RuntimeService runtimeService,
      ObjectConverter objectConverter) {
    super(repositoryService, historyService, runtimeService, objectConverter);
  }

  @Override
  public VariablesDomain findGlobalByWorkflowInstanceId(String id) {
    HistoricVariableInstanceEntity variables = (HistoricVariableInstanceEntity)historyService.createHistoricVariableInstanceQuery().processInstanceId(id).variableName("variables").singleResult();
    VariablesDomain variablesDomain = new VariablesDomain();
    variablesDomain.setOutputs((Map<String, Object>)variables.getValue());
    variablesDomain.setRevision(variables.getRevision());
    variablesDomain.setUpdateTime(variables.getCreateTime().toInstant());
    return variablesDomain;
  }
}
