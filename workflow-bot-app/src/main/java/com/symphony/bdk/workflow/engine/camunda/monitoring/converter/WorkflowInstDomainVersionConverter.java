package com.symphony.bdk.workflow.engine.camunda.monitoring.converter;

import com.symphony.bdk.workflow.converter.BiConverter;
import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowInstanceDomain;

import org.camunda.bpm.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class WorkflowInstDomainVersionConverter extends AbstractInstanceDomainConverter
    implements BiConverter<HistoricProcessInstanceEntity, Map<String, String>, WorkflowInstanceDomain> {

  @Override
  public WorkflowInstanceDomain apply(HistoricProcessInstanceEntity hisProcInstance,
      Map<String, String> procIdVersionTagMap) {
    Optional<String> version = Optional.ofNullable(procIdVersionTagMap.get(hisProcInstance.getProcessDefinitionId()));
    return version.map(v -> this.instanceCommonBuilder(hisProcInstance).version(Long.valueOf(v)).build()).orElse(null);
  }
}
