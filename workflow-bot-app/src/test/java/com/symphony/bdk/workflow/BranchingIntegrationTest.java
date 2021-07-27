package com.symphony.bdk.workflow;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.symphony.bdk.workflow.lang.WorkflowBuilder;
import com.symphony.bdk.workflow.lang.swadl.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

class BranchingIntegrationTest extends IntegrationTest {

  static Stream<Arguments> executedActivities() {
    return Stream.of(
        arguments("/branching/if.swadl.yaml", List.of("act1", "act2")),
        arguments("/branching/if-else-end.swadl.yaml", List.of("act1")),
        arguments("/branching/if-else-activity.swadl.yaml", List.of("act1", "act3")),
        arguments("/branching/if-else-if.swadl.yaml", List.of("act1", "act3")),
        arguments("/branching/if-nested.swadl.yaml", List.of("act1", "act2", "act2-2")),
        arguments("/branching/if-else-nested.swadl.yaml", List.of("act1", "act2", "act2-3")),
        arguments("/branching/if-else-more-activities.swadl.yaml", List.of("act1", "act3", "act3-2"))
    );
  }

  @ParameterizedTest
  @MethodSource("executedActivities")
  void branching(String workflowFile, List<String> activities) throws IOException, ProcessingException {
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
