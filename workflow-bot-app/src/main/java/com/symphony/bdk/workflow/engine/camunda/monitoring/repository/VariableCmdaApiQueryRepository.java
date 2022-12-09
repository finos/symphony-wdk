package com.symphony.bdk.workflow.engine.camunda.monitoring.repository;

import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.monitoring.repository.VariableQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.domain.VariablesDomain;

import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricDetail;
import org.camunda.bpm.engine.history.HistoricDetailQuery;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class VariableCmdaApiQueryRepository extends CamundaAbstractQueryRepository implements VariableQueryRepository {
  public VariableCmdaApiQueryRepository(RepositoryService repositoryService, HistoryService historyService,
      RuntimeService runtimeService, ManagementService managementService, ObjectConverter objectConverter) {
    super(repositoryService, historyService, runtimeService, managementService, objectConverter);
  }

  @Override
  public VariablesDomain findVarsByWorkflowInstanceIdAndVarName(String id, String name) {
    HistoricVariableInstanceEntity variables =
        (HistoricVariableInstanceEntity) historyService.createHistoricVariableInstanceQuery()
            .processInstanceId(id)
            .variableName(name)
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
  public List<VariablesDomain> findGlobalVarsHistoryByWorkflowInstId(String id, Instant updatedBefore,
      Instant updatedAfter) {
    HistoricVariableInstance variables = historyService.createHistoricVariableInstanceQuery()
        .variableName("variables")
        .processInstanceId(id)
        .singleResult();

    if (variables == null) {
      return Collections.emptyList();
    }

    String varId = variables.getId();

    HistoricDetailQuery historicDetailQuery = historyService.createHistoricDetailQuery()
        .processInstanceId(id)
        .variableInstanceId(varId);


    if (updatedBefore != null) {
      historicDetailQuery = historicDetailQuery.occurredBefore(Date.from(updatedBefore));
    }

    if (updatedAfter != null) {
      historicDetailQuery = historicDetailQuery.occurredAfter(Date.from(updatedAfter));
    }

    List<HistoricDetail> historicDetails = historicDetailQuery
        .orderByVariableRevision()
        .asc()
        .list();

    return objectConverter.convertCollection(historicDetails, HistoricDetail.class, VariablesDomain.class);
  }
}
