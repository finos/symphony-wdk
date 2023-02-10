package com.symphony.bdk.workflow.engine.camunda.monitoring.converter;

import com.symphony.bdk.workflow.converter.Converter;
import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowInstanceDomain;

import org.camunda.bpm.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.springframework.stereotype.Component;

@Component
public class WorkflowInstDomainConverter extends AbstractInstanceDomainConverter
    implements Converter<HistoricProcessInstanceEntity, WorkflowInstanceDomain> {

  @Override
  public WorkflowInstanceDomain apply(HistoricProcessInstanceEntity hisProcInstance) {
    return this.instanceCommonBuilder(hisProcInstance).build();
  }
}
