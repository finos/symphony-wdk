package com.symphony.bdk.workflow.monitoring.service.converter;

import static org.assertj.core.api.BDDAssertions.then;

import com.symphony.bdk.workflow.api.v1.dto.NodeStateView;
import com.symphony.bdk.workflow.monitoring.repository.domain.ActivityInstanceDomain;
import com.symphony.bdk.workflow.monitoring.repository.domain.VariablesDomain;

import org.assertj.core.util.Maps;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

class ActivityInstViewConverterTest {

  @ParameterizedTest
  @ValueSource(strings = {"scriptTask", "serviceTask"})
  void apply_convertTask(String task) {
    // given
    Instant start = Instant.now();
    Instant end = Instant.now().plus(5, ChronoUnit.MINUTES);
    VariablesDomain vars = new VariablesDomain();
    vars.setRevision(3);
    vars.setUpdateTime(Instant.now());
    vars.setOutputs(Maps.newHashMap("key", "value"));
    ActivityInstanceDomain domain = ActivityInstanceDomain.builder()
        .id("id")
        .name("activity")
        .procInstId("process-instance-id")
        .workflowId("workflow")
        .variables(vars)
        .startDate(start)
        .endDate(end)
        .duration(Duration.between(start, end))
        .type(task)
        .build();

    // when
    ActivityInstViewConverter converter = new ActivityInstViewConverter();
    NodeStateView instanceView = converter.apply(domain);

    // then
    then(instanceView.getNodeId()).isEqualTo("activity");
    then(instanceView.getInstanceId()).isEqualTo("process-instance-id");
    then(instanceView.getWorkflowId()).isEqualTo("workflow");
    then(instanceView.getStartDate()).isEqualTo(start);
    then(instanceView.getEndDate()).isEqualTo(end);
    then(instanceView.getDuration()).isEqualTo(Duration.between(start, end));
    then(instanceView.getOutputs()).hasSize(1);
  }

}
