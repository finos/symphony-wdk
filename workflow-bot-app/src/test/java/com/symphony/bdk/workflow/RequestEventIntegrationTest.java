package com.symphony.bdk.workflow;

import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.content;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.workflow.engine.ExecutionParameters;
import com.symphony.bdk.workflow.engine.UnauthorizedException;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

class RequestEventIntegrationTest extends IntegrationTest {

  @Test
  void onRequestReceived() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/event/request-received.swadl.yaml"));

    engine.deploy(workflow);
    engine.execute("my-workflow", new ExecutionParameters(Map.of("content", "Hello World!"), "myToken"));
    verify(messageService, timeout(5000).times(1)).send(eq("123"), content("Hello World!"));
  }

  static Stream<Arguments> errorsStream() {
    return Stream.of(
        arguments("my-workflow", new ExecutionParameters(Map.of("content", "Hello World!"), "badToken"),
            UnauthorizedException.class,
            "Request token is not valid"),
        arguments("my-workflow", new ExecutionParameters(Map.of("content", "Hello World!"), null),
            UnauthorizedException.class,
            "Request token is not valid"),
        arguments("unfoundWorkflowId", new ExecutionParameters(Map.of("content", "Hello World!"), "myToken"),
            IllegalArgumentException.class,
            "No workflow found with id unfoundWorkflowId")
    );
  }

  @ParameterizedTest
  @MethodSource("errorsStream")
  void onRequestReceived_badToken(String workflowId, ExecutionParameters executionParameters,
      Class<? extends Throwable> expectedExceptionType, String errorMessage) throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/event/request-received.swadl.yaml"));

    engine.deploy(workflow);
    assertThatExceptionOfType(expectedExceptionType).isThrownBy(
            () -> engine.execute(workflowId, executionParameters))
        .satisfies(e -> assertThat(e.getMessage()).isEqualTo(errorMessage));
    verify(messageService, never()).send(anyString(), any(Message.class));
  }

}
