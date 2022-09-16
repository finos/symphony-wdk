package com.symphony.bdk.workflow.engine.camunda.monitoring.converter;

import static org.assertj.core.api.BDDAssertions.then;

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
    then(domain.getVersion()).isEqualTo(4);
    then(domain.getId()).isEqualTo("id");
    then(domain.getName()).isEqualTo("workflow");
    then(domain.getInstanceId()).isEqualTo("instance-id");
    then(domain.getStatus()).isEqualTo("COMPLETE");
    then(domain.getStartDate()).isEqualTo(start.toInstant());
    then(domain.getEndDate()).isEqualTo(end.toInstant());
    then(domain.getDuration()).isEqualTo(duration);
  }
}
