package com.symphony.bdk.workflow;

import static com.symphony.bdk.workflow.custom.assertion.Assertions.assertThat;
import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.content;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.workflow.engine.ExecutionParameters;
import com.symphony.bdk.workflow.exception.UnauthorizedException;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

class RequestReceivedEventIntegrationTest extends IntegrationTest {

  @Test
  void onRequestReceived() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/event/request-received.swadl.yaml"));
    engine.deploy(workflow);

    engine.execute("request-received", new ExecutionParameters(Map.of("content", "Hello World!"), "myToken"));

    verify(messageService, timeout(5000).times(1)).send(eq("123"), content("Hello World!"));
  }

  @Test
  void onRequestReceived_badToken() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/event/request-received.swadl.yaml"));

    engine.deploy(workflow);

    ExecutionParameters executionParameters = new ExecutionParameters(Map.of("content", "Hello World!"), "badToken");
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(
            () -> engine.execute("request-received", executionParameters))
        .satisfies(e -> assertThat(e.getMessage()).isEqualTo("Request is not authorised"));
    verify(messageService, never()).send(anyString(), any(Message.class));
  }

  @Test
  void onRequestReceived_tokenNull() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/event/request-received.swadl.yaml"));

    engine.deploy(workflow);

    ExecutionParameters executionParameters = new ExecutionParameters(Map.of("content", "Hello World!"), null);
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(
            () -> engine.execute("request-received", executionParameters))
        .satisfies(e -> assertThat(e.getMessage()).isEqualTo("Request is not authorised"));
    verify(messageService, never()).send(anyString(), any(Message.class));
  }

  @Test
  void onRequestReceived_badWorkflowId() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/event/request-received.swadl.yaml"));

    engine.deploy(workflow);

    ExecutionParameters executionParameters = new ExecutionParameters(Map.of("content", "Hello World!"), "myToken");
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
            () -> engine.execute("unfoundWorkflowId", executionParameters))
        .satisfies(e -> assertThat(e.getMessage()).isEqualTo("No workflow found with id unfoundWorkflowId"));
    verify(messageService, never()).send(anyString(), any(Message.class));
  }

  @Test
  void onRequestReceived_multipleWorkflows() throws IOException, ProcessingException {
    final Workflow workflow1 =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/event/request-received.swadl.yaml"));
    engine.deploy(workflow1);

    final Workflow workflow2 =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/event/request-received2.swadl.yaml"));
    engine.deploy(workflow2);

    // should only execute workflow1
    engine.execute("request-received", new ExecutionParameters(Map.of("content", "Hello World!"), "myToken"));

    assertThat(workflow1).isExecuted();
    verify(messageService, never()).send(anyString(), content("Second"));
  }

}
