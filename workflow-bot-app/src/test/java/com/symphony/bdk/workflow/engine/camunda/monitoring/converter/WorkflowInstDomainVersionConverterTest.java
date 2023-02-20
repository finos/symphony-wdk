package com.symphony.bdk.workflow.engine.camunda.monitoring.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowInstanceDomain;

import org.camunda.bpm.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.util.Date;
import java.util.Map;

class WorkflowInstDomainVersionConverterTest {

  @ParameterizedTest
  @CsvSource({"PENDING, PENDING, ", "COMPLETED, COMPLETED, endEvent", "COMPLETED, FAILED, notEndEvent", "UNKNOWN,,"})
  void apply(String sourceState, String targetState, String endActivityId) {
    // given
    HistoricProcessInstanceEntity entity = new HistoricProcessInstanceEntity();
    entity.setId("id");
    entity.setProcessDefinitionKey("workflow");
    entity.setProcessInstanceId("instance-id");
    entity.setProcessDefinitionId("definition-id4");
    entity.setProcessDefinitionVersion(4);
    Date start = new Date();
    entity.setStartTime(start);
    entity.setState(sourceState);
    entity.setEndActivityId(endActivityId);
    Date end = new Date();
    entity.setEndTime(end);
    Duration duration = Duration.between(start.toInstant(), end.toInstant());
    entity.setDurationInMillis(duration.toMillis());

    // when
    WorkflowInstDomainVersionConverter converter = new WorkflowInstDomainVersionConverter();
    WorkflowInstanceDomain domain = converter.apply(entity, Map.of("definition-id4", "1674651222294886"));

    // then
    assertThat(domain.getVersion()).isEqualTo(1674651222294886L);
    assertThat(domain.getId()).isEqualTo("id");
    assertThat(domain.getName()).isEqualTo("workflow");
    assertThat(domain.getInstanceId()).isEqualTo("instance-id");
    assertThat(domain.getStatus()).isEqualTo(targetState);
    assertThat(domain.getStartDate()).isEqualTo(start.toInstant());
    assertThat(domain.getEndDate()).isEqualTo(end.toInstant());
    assertThat(domain.getDuration()).isEqualTo(duration);
  }
}
