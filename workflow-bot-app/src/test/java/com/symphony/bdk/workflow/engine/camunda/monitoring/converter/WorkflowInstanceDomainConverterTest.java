package com.symphony.bdk.workflow.engine.camunda.monitoring.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowInstanceDomain;

import org.camunda.bpm.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Date;

class WorkflowInstanceDomainConverterTest {

  @Test
  void apply() {
    // given
    HistoricProcessInstanceEntity entity = new HistoricProcessInstanceEntity();
    entity.setId("id");
    entity.setProcessDefinitionKey("workflow");
    entity.setProcessInstanceId("instance-id");
    entity.setProcessDefinitionVersion(4);
    Date start = new Date();
    entity.setStartTime(start);
    entity.setState("COMPLETE");
    Date end = new Date();
    entity.setEndTime(end);
    Duration duration = Duration.between(start.toInstant(), end.toInstant());
    entity.setDurationInMillis(duration.toMillis());

    // when
    WorkflowInstanceDomainConverter converter = new WorkflowInstanceDomainConverter();
    WorkflowInstanceDomain domain = converter.apply(entity);

    // then
    assertThat(domain.getVersion()).isEqualTo(4);
    assertThat(domain.getId()).isEqualTo("id");
    assertThat(domain.getName()).isEqualTo("workflow");
    assertThat(domain.getInstanceId()).isEqualTo("instance-id");
    assertThat(domain.getStatus()).isEqualTo("COMPLETE");
    assertThat(domain.getStartDate()).isEqualTo(start.toInstant());
    assertThat(domain.getEndDate()).isEqualTo(end.toInstant());
    assertThat(domain.getDuration()).isEqualTo(duration);
  }
}
