package com.symphony.bdk.workflow.engine.camunda.monitoring.converter;

import com.symphony.bdk.workflow.converter.Converter;
import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowInstanceDomain;

import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class WorkflowInstanceDomainConverter implements Converter<HistoricProcessInstanceEntity, WorkflowInstanceDomain> {

  @Override
  public WorkflowInstanceDomain apply(HistoricProcessInstanceEntity historicProcessInstance) {
    return WorkflowInstanceDomain.builder()
        .id(historicProcessInstance.getId())
        .name(historicProcessInstance.getProcessDefinitionKey())
        .instanceId(historicProcessInstance.getId())
        .version(historicProcessInstance.getProcessDefinitionVersion())
        .startDate(historicProcessInstance.getStartTime().toInstant())
        .endDate(historicProcessInstance.getEndTime() == null ? null : historicProcessInstance.getEndTime().toInstant())
        .status(historicProcessInstance.getState())
        .duration(historicProcessInstance.getDurationInMillis() == null ? null
            : Duration.ofMillis(historicProcessInstance.getDurationInMillis()))
        .build();
  }
}
