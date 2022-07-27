package com.symphony.bdk.workflow;

import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.exception.ActivityNotFoundException;
import com.symphony.bdk.workflow.swadl.exception.InvalidActivityException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

class BranchingIntegrationTest extends IntegrationTest {

  static Stream<Arguments> executedActivities() {
    return Stream.of(
        arguments("/branching/if.swadl.yaml", List.of("act1", "act2")),
        arguments("/branching/if-else-end.swadl.yaml", List.of("act1")),
        arguments("/branching/if-else-activity.swadl.yaml", List.of("act1", "act3")),
        arguments("/branching/if-else-if.swadl.yaml", List.of("act1", "act3")),
        arguments("/branching/if-nested.swadl.yaml", List.of("act1", "act2", "act2_2")),
        arguments("/branching/if-else-nested.swadl.yaml", List.of("act1", "act2", "act2_3")),
        arguments("/branching/if-join.swadl.yaml", List.of("act1", "act2", "act4")),
        arguments("/branching/second-if-join.swadl.yaml", List.of("act1", "act3", "act4")),
        arguments("/branching/if-else-join.swadl.yaml", List.of("act1", "act3", "act4")),
        arguments("/branching/if-join-continue.swadl.yaml", List.of("act1", "act2", "act4", "act5")),
        arguments("/branching/if-else-more-activities.swadl.yaml", List.of("act1", "act3", "act3_2"))
    );
  }

  @SuppressWarnings("checkstyle:LineLength")
  static Stream<Arguments> expectedErrors() {
    return Stream.of(
        arguments("/branching/invalid/swadl/if-starting-activity.swadl.yaml", InvalidActivityException.class,
            "Invalid activity in the workflow if-in-starting-activity: Starting activity startingActivity cannot have a conditional branching"),
        arguments("/branching/invalid/swadl/else-without-if.swadl.yaml", InvalidActivityException.class,
            "Invalid activity in the workflow else-without-if: Expecting \"if\" keyword to open a new conditional branching, got \"else\""),
//        arguments("/branching/invalid/swadl/else-on-activity-completed-with-branching.swadl.yaml",
//            InvalidActivityException.class,
//            "Invalid activity in the workflow else-on-activity-completed-with-branching: Expecting activity ac2 not to have a parent activity with conditional branching, got ac1"),
        arguments("/branching/invalid/swadl/else-without-on-activity-completed.swadl.yaml",
            InvalidActivityException.class,
            "Invalid activity in the workflow else-without-on-activity-completed: Expecting \"if\" keyword to open a new conditional branching, got \"else\""),
        arguments("/branching/invalid/swadl/else-with-unknown-on-activity-completed.swadl.yaml",
            ActivityNotFoundException.class,
            "Invalid activity in the workflow else-with-unknown-on-activity-completed: No activity found with id unknown-id referenced in ac2")
    );
  }

  @ParameterizedTest
  @MethodSource("executedActivities")
  void branching(String workflowFile, List<String> activities) throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(workflowFile));
    engine.deploy(workflow);

    engine.onEvent(messageReceived("/execute"));

    assertThat(workflow).isExecutedWithProcessAndActivities(lastProcess(), activities);
  }

  @ParameterizedTest
  @MethodSource("expectedErrors")
  void branchingWithError(String workflowFile, Class<? extends Throwable> expectedExceptionType, String error)
      throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(workflowFile));

    assertThatExceptionOfType(expectedExceptionType)
        .isThrownBy(() -> engine.deploy(workflow))
        .satisfies(e -> assertThat(e.getMessage()).isEqualTo(error));
  }

}
