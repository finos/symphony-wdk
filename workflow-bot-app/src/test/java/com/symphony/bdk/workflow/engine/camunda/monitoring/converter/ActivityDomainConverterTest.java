package com.symphony.bdk.workflow.engine.camunda.monitoring.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphony.bdk.workflow.monitoring.repository.domain.ActivityInstanceDomain;

import org.camunda.bpm.engine.impl.persistence.entity.HistoricActivityInstanceEntity;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Date;

class ActivityDomainConverterTest {

  @Test
  void apply() {
    // given
    Date start = new Date();
    HistoricActivityInstanceEntity entity = new HistoricActivityInstanceEntity();
    entity.setActivityId("activity");
    entity.setActivityInstanceId("activity-instance-id");
    entity.setActivityName("activity");
    entity.setActivityType("scriptTask");
    entity.setId("id");
    entity.setProcessDefinitionKey("workflow");
    entity.setProcessInstanceId("process-instance-id");
    entity.setStartTime(start);
    Date end = new Date();
    entity.setEndTime(end);
    Duration duration = Duration.between(start.toInstant(), end.toInstant());
    entity.setDurationInMillis(duration.toMillis());

    // when
    ActivityDomainConverter converter = new ActivityDomainConverter();
    ActivityInstanceDomain domain = converter.apply(entity);

    // then
    assertThat(domain.getType()).isEqualTo("scriptTask");
    assertThat(domain.getId()).isEqualTo("id");
    assertThat(domain.getName()).isEqualTo("activity");
    assertThat(domain.getProcInstId()).isEqualTo("process-instance-id");
    assertThat(domain.getWorkflowId()).isEqualTo("workflow");
    assertThat(domain.getStartDate()).isEqualTo(start.toInstant());
    assertThat(domain.getEndDate()).isEqualTo(end.toInstant());
    assertThat(domain.getDuration()).isEqualTo(duration);
  }
}
