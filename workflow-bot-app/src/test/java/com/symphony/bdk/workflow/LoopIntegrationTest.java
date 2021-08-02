package com.symphony.bdk.workflow;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.symphony.bdk.workflow.swadl.WorkflowBuilder;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

class LoopIntegrationTest extends IntegrationTest {

  static Stream<Arguments> executedActivities() {
    return Stream.of(
        arguments("/loop/loop.swadl.yaml", List.of("act1", "act2", "act1", "act2")),
        arguments("/loop/loop-continue-activity.swadl.yaml", List.of("act1", "act2", "act1", "act2", "act3")),
        arguments("/loop/loop-more-activities.swadl.yaml", List.of("act1", "act2", "act3", "act1", "act2", "act3"))
    );
  }

  @ParameterizedTest
  @MethodSource("executedActivities")
  void looping(String workflowFile, List<String> activities) throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(workflowFile));
    engine.execute(workflow);

    Optional<String> process = engine.onEvent(messageReceived("/execute"));

    assertExecuted(process, activities);
  }

  private void assertExecuted(Optional<String> process, List<String> activities) {
    assertThat(process).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));

    List<HistoricActivityInstance> processes = historyService.createHistoricActivityInstanceQuery()
        .processInstanceId(process.get())
        .activityType("scriptTask")
        .orderByHistoricActivityInstanceStartTime().asc()
        .list();

    assertThat(processes)
        .extracting(HistoricActivityInstance::getActivityName)
        .containsExactly(activities.toArray(String[]::new));
  }
}
