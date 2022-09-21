package com.symphony.bdk.workflow.engine.camunda.monitoring.repository;

import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.monitoring.repository.VariableQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.domain.VariablesDomain;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricDetail;
import org.camunda.bpm.engine.history.HistoricDetailQuery;
import org.camunda.bpm.engine.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class VariableCmdaApiQueryRepository extends CamundaAbstractQueryRepository implements VariableQueryRepository {
  public VariableCmdaApiQueryRepository(RepositoryService repositoryService,
      HistoryService historyService, RuntimeService runtimeService,
      ObjectConverter objectConverter) {
    super(repositoryService, historyService, runtimeService, objectConverter);
  }

  @Override
  public VariablesDomain findGlobalVarsByWorkflowInstanceId(String id) {
    HistoricVariableInstanceEntity variables =
        (HistoricVariableInstanceEntity) historyService.createHistoricVariableInstanceQuery()
            .processInstanceId(id)
            .variableName("variables")
            .singleResult();
    VariablesDomain variablesDomain = new VariablesDomain();

    if (variables != null) {
      variablesDomain.setOutputs((Map<String, Object>) variables.getValue());
      variablesDomain.setRevision(variables.getRevision());
      variablesDomain.setUpdateTime(variables.getCreateTime().toInstant());
    }

    return variablesDomain;
  }

  @Override
  public List<VariablesDomain> findGlobalVarsHistoryByWorkflowInstId(String id, Long occurredBefore,
      Long occurredAfter) {
    String varId = historyService.createHistoricVariableInstanceQuery()
        .variableName("variables")
        .processInstanceId(id)
        .singleResult()
        .getId();

    HistoricDetailQuery historicDetailQuery = historyService.createHistoricDetailQuery()
        .processInstanceId(id)
        .variableInstanceId(varId);


    if (occurredBefore != null) {
      historicDetailQuery = historicDetailQuery.occurredBefore(new Date(occurredBefore));
    }

    if (occurredAfter != null) {
      historicDetailQuery = historicDetailQuery.occurredAfter(new Date(occurredAfter));
    }

    List<HistoricDetail> historicDetails = historicDetailQuery
        .orderByVariableRevision()
        .asc()
        .list();

    return objectConverter.convertCollection(historicDetails, HistoricDetail.class, VariablesDomain.class);
  }
}
