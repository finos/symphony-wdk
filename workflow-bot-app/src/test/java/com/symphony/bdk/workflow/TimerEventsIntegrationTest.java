package com.symphony.bdk.workflow;

import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.content;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

// Tests can be fragile if as they are time based
class TimerEventsIntegrationTest extends IntegrationTest {

  @Test
  void at() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/timer/timer-at.swadl.yaml"));

    // set workflow to execute once in the future
    Instant future = Instant.now().plus(1, ChronoUnit.SECONDS);
    workflow.getFirstActivity().get().getEvent().get().getTimerFired().setAt(future.toString());
    engine.deploy(workflow);

    // wait for execution
    verify(messageService, timeout(5000)).send(eq("abc"), content("Ok"));
  }

  @Test
  void repeat() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/timer/timer-repeat.swadl.yaml"));

    engine.deploy(workflow);

    // wait for multiple executions
    verify(messageService, timeout(5000).times(2)).send(eq("abc"), content("Ok"));
  }

  @Test
  void repeatAsIntermediateEvent() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/timer/timer-repeat-intermediate.swadl.yaml"));
    when(messageService.send(anyString(), any(Message.class))).thenReturn(message("msgId"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/execute"));

    verify(messageService, timeout(5000)).send(eq("abc"), content("start"));
    // executed only once because the process ends after
    verify(messageService, timeout(5000)).send(eq("abc"), content("repeat"));
  }

  @Test
  void repeatMixedWithOtherEvents() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/timer/timer-repeat-mixed.swadl.yaml"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/execute"));

    // wait for multiple executions: 1 with message, 2 by timer
    verify(messageService, timeout(5000).times(3)).send(eq("abc"), content("Ok"));
  }

}
