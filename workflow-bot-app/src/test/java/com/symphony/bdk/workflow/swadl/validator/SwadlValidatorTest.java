package com.symphony.bdk.workflow.swadl.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.symphony.bdk.workflow.swadl.WorkflowBuilder;
import com.symphony.bdk.workflow.swadl.exception.SwadlNotValidException;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

class SwadlValidatorTest {

  static Stream<Arguments> expectedErrors() {
    return Stream.of(
        arguments("unknown-properties.swadl.yaml", List.of(Pair.of(2, "Unknown property 'unknown'"))),

        // field is missing so no lines to reported
        arguments("missing-properties.swadl.yaml",
            List.of(Pair.of(-1, "Missing property 'activities' for root object"))),

        arguments("null-properties.swadl.yaml",
            List.of(Pair.of(1, "Invalid property 'name', expecting string type, got null"))),

        arguments("wrong-type-properties.swadl.yaml",
            List.of(Pair.of(2, "Invalid property 'version', expecting string type, got integer"))),

        arguments("multiple-errors.swadl.yaml",
            List.of(Pair.of(1, "Invalid property 'name', expecting string type, got null"),
                Pair.of(2, "Invalid property 'version', expecting string type, got integer"))),

        arguments("unknown-activity.swadl.yaml",
            List.of(Pair.of(3, "Unknown property 'send-message-unknown'"))),

        // escaping the expected pattern as we are matching over a pattern in the assertions
        arguments("invalid-id-property.swadl.yaml",
            List.of(Pair.of(4, "Invalid property 'id', must match pattern \\^\\[a-zA-Z0-9_\\]\\+\\$"))),

        arguments("missing-property-activity.swadl.yaml",
            List.of(Pair.of(3, "Missing property 'id' for send-message object"))),

        arguments("invalid-property-activity.swadl.yaml",
            List.of(Pair.of(4, "Invalid property 'id', expecting string type, got integer"))),

        arguments("empty-on-event.swadl.yaml",
            List.of(Pair.of(6, "Invalid property 'on', expecting object type, got null"))),

        arguments("null-variables.swadl.yaml",
            List.of(Pair.of(2, "Invalid property 'variables', expecting object type, got null"))),

        arguments("empty-activities.swadl.yaml",
            List.of(Pair.of(2, "Invalid property 'activities', expecting array type, got null"))),

        arguments("unknown-on-event.swadl.yaml",
            List.of(Pair.of(4, "Invalid property 'unknown', expecting object type, got string"))),

        arguments("message-without-content.swadl.yaml",
            List.of(Pair.of(3, "Invalid property 'send-message', expecting object type, got null"))),

        arguments("invalid-empty-activity.swadl.yaml",
            List.of(Pair.of(3,
                "Invalid property 'empty', expecting object type, got string"))),

        arguments("invalid-event-activity.swadl.yaml",
            List.of(Pair.of(6, "Unknown property 'message-received-unknown' for on object")))
    );
  }

  @ParameterizedTest
  @MethodSource("expectedErrors")
  void validate(String workflowFile, List<Pair<Integer, String>> errors) {
    assertThatExceptionOfType(SwadlNotValidException.class)
        .isThrownBy(() ->
            WorkflowBuilder.fromYaml(getClass().getResourceAsStream(workflowFile)))
        .satisfies(e -> {
          for (int i = 0; i < errors.size(); i++) {
            Pair<Integer, String> error = errors.get(i);
            assertThat(e.getErrors().get(i).getLineNumber()).isEqualTo(error.getLeft());
            assertThat(e.getErrors().get(i).getMessage()).matches(error.getRight());
          }
        });
  }
}
