package com.symphony.bdk.workflow.engine.camunda.monitoring.converter;

import com.symphony.bdk.workflow.converter.Converter;
import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowDomain;

import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class WorkflowDomainConverter implements Converter<ProcessDefinitionEntity, WorkflowDomain> {

  @Override
  public WorkflowDomain apply(ProcessDefinitionEntity processDefinition) {
    return WorkflowDomain.builder()
        .id(processDefinition.getId())
        .name(processDefinition.getName())
        .version(Optional.ofNullable(processDefinition.getVersionTag()).map(Long::valueOf).orElse(null))
        .build();
  }
}
