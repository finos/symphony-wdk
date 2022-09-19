package com.symphony.bdk.workflow.engine.camunda.monitoring.converter;

import static org.assertj.core.api.Assertions.assertThat;

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
    assertThat(domain.getVersion()).isEqualTo(2);
    assertThat(domain.getId()).isEqualTo("id");
    assertThat(domain.getName()).isEqualTo("workflow");
  }
}
