package com.symphony.bdk.workflow.monitoring.service.converter;

import static org.assertj.core.api.BDDAssertions.then;

import com.symphony.bdk.workflow.api.v1.dto.StatusEnum;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstView;
import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowInstanceDomain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

class WorkflowInstViewConverterTest {

  @ParameterizedTest
  @CsvSource(value = {"ACTIVE,PENDING", ",COMPLETED"})
  void apply(String status, String expected) {
    // given
    Instant start = Instant.now();
    Instant end = Instant.now().plus(5, ChronoUnit.MINUTES);
    WorkflowInstanceDomain domain = WorkflowInstanceDomain.builder().name("activity").instanceId("inst-id").duration(
        Duration.ofMinutes(1)).status(status).startDate(start).endDate(end).version(1).build();

    // when
    WorkflowInstViewConverter converter = new WorkflowInstViewConverter();
    WorkflowInstView instView = converter.apply(domain);

    // then
    then(instView.getId()).isEqualTo("activity");
    then(instView.getInstanceId()).isEqualTo("inst-id");
    then(instView.getStatus()).isEqualTo(StatusEnum.valueOf(expected));
    then(instView.getStartDate()).isEqualTo(start);
    then(instView.getEndDate()).isEqualTo(end);
    then(instView.getDuration()).isEqualTo(Duration.ofMinutes(1));
    then(instView.getVersion()).isEqualTo(1);
  }
}
