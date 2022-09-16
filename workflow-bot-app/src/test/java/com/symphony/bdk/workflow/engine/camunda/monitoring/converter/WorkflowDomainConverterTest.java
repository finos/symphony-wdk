package com.symphony.bdk.workflow.engine.camunda.monitoring.converter;

import static org.assertj.core.api.BDDAssertions.then;

import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowDomain;

import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.junit.jupiter.api.Test;

class WorkflowDomainConverterTest {
  @Test
  void apply() {
    // given
    ProcessDefinitionEntity entity = new ProcessDefinitionEntity();
    entity.setVersion(2);
    entity.setId("id");
    entity.setName("workflow");

    // when
    WorkflowDomainConverter converter = new WorkflowDomainConverter();
    WorkflowDomain domain = converter.apply(entity);

    // then
    then(domain.getVersion()).isEqualTo(2);
    then(domain.getId()).isEqualTo("id");
    then(domain.getName()).isEqualTo("workflow");
  }
}
