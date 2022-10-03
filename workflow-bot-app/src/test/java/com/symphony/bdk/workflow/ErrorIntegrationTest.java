package com.symphony.bdk.workflow;

import static com.symphony.bdk.workflow.custom.assertion.Assertions.assertThat;
import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.content;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class ErrorIntegrationTest extends IntegrationTest {

  @Test
  void onActivityFailed() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/error/on-activity-failed.swadl.yaml"));
    when(messageService.send(eq("STREAM"), content("On success"))).thenThrow(new RuntimeException("Failure"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/failure"));

    verify(messageService, timeout(5000)).send(eq("STREAM"), content("On failure"));
    assertThat(workflow).executed("fallback");
  }

  @Test
  void onActivityFailedContinue() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/error/on-activity-failed-continue.swadl.yaml"));
    when(messageService.send(eq("STREAM"), content("On success"))).thenThrow(new RuntimeException("Failure"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/failure"));

    verify(messageService, timeout(5000)).send(eq("STREAM"), content("On failure"));
    assertThat(workflow).executed("fallback");
  }

  @Test
  void onActivityFailedContinue2() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/error/on-activity-failed-continue2.swadl.yaml"));
    when(messageService.send(eq("STREAM"), content("On success"))).thenThrow(new RuntimeException("Failure"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/failure"));

    verify(messageService, timeout(5000)).send(eq("STREAM"), content("On failure"));
    assertThat(workflow).executed("fallback");
  }

  @Test
  void onActivityFailedContinueFailure() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/error/on-activity-failed-continue-failure.swadl.yaml"));
    when(messageService.send(anyString(), any(Message.class))).thenReturn(message("msgId"));
    when(messageService.send(eq("STREAM"), content("On success"))).thenThrow(new RuntimeException("Failure"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/failure"));

    verify(messageService, timeout(5000)).send(eq("STREAM"), content("On failure"));
    verify(messageService, timeout(5000)).send(eq("STREAM"), content("On failure 2"));
    assertThat(workflow).executed("continue", "continue2");
  }

  @Test
  void onActivityFailed_notFailed() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/error/on-activity-failed.swadl.yaml"));

    when(messageService.send(anyString(), any(Message.class))).thenReturn(message("msgId"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/failure"));

    verify(messageService, timeout(5000)).send(eq("STREAM"), content("On success"));
    assertThat(workflow).executed("failing");
  }

  @Test
  void onActivityFailed_OneOf_SecondFails() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/error/on-activity-failed-one-of.swadl.yaml"));
    when(messageService.send(anyString(), any(Message.class))).thenReturn(message("msgId"));
    when(messageService.send(eq("STREAM"), content("Second"))).thenThrow(new RuntimeException("Failure"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/failure"));

    verify(messageService, timeout(5000)).send(eq("STREAM"), content("On failure"));
    assertThat(workflow).executed("first", "fallback");
  }

  @Test
  void onActivityFailed_OneOf_FirstFails() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/error/on-activity-failed-one-of.swadl.yaml"));
    when(messageService.send(eq("STREAM"), content("First"))).thenThrow(new RuntimeException("Failure"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/failure"));

    verify(messageService, timeout(5000)).send(eq("STREAM"), content("On failure"));
    assertThat(workflow).executed("fallback");
  }

  @Test
  void onScriptActivityFailed() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/error/on-script-activity-failed.swadl.yaml"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/failure"));

    verify(messageService, timeout(5000)).send(eq("STREAM"), content("On failure"));
    assertThat(workflow).executed("fallback");
  }

  @Test
  void onActivityFailedRetry() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/error/on-activity-failed-retry.swadl.yaml"));

    when(messageService.send(anyString(), any(Message.class))).thenReturn(message("msgId"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/failure"));

    verify(messageService, timeout(5000)).send(eq("STREAM"), content("On failure"));
    assertThat(workflow).executed("fallback", "failing");
  }

  @Test
  void sendMessageOnMessage_withException_errorVarNotEmpty() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/send-message-on-message.swadl.yaml"));

    RuntimeException toBeThrown = mock(RuntimeException.class);
    when(toBeThrown.getMessage()).thenReturn("Exception");
    doThrow(toBeThrown).when(messageService).send(anyString(), any(Message.class));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/message"));

    sleepToTimeout(500);
    Map<String, Object> errors = getVariable(lastProcess().get(), "error");
    Assertions.assertThat(errors.get("message")).isEqualTo("Exception");
    Assertions.assertThat(errors.get("activityId")).isEqualTo("sendMessage1");
  }

}
