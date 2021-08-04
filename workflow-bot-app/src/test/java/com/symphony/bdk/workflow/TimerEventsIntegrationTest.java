package com.symphony.bdk.workflow;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.workflow.swadl.WorkflowBuilder;
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
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/events/timer/timer-at.swadl.yaml"));
    when(messageService.send(anyString(), anyString())).thenReturn(message("msgId"));

    // set workflow to execute once in the future
    Instant future = Instant.now().plus(1, ChronoUnit.SECONDS);
    workflow.getFirstActivity().get().getEvent().get().getTimerFired().setAt(future.toString());
    engine.execute(workflow);

    // wait for execution
    verify(messageService, timeout(5000)).send("abc", "Ok");
  }

  @Test
  void repeat() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/events/timer/timer-repeat.swadl.yaml"));
    when(messageService.send(anyString(), anyString())).thenReturn(message("msgId"));

    engine.execute(workflow);

    // wait for multiple executions
    verify(messageService, timeout(5000).times(2)).send("abc", "Ok");
  }

  @Test
  void repeatAsIntermediateEvent() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/events/timer/timer-repeat-intermediate.swadl.yaml"));
    when(messageService.send(anyString(), anyString())).thenReturn(message("msgId"));

    engine.execute(workflow);
    engine.onEvent(messageReceived("/execute"));

    verify(messageService, timeout(5000)).send("abc", "start");
    // executed only once because the process ends after
    verify(messageService, timeout(5000)).send("abc", "repeat");
  }

  @Test
  void repeatMixedWithOtherEvents() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/events/timer/timer-repeat-mixed.swadl.yaml"));
    when(messageService.send(anyString(), anyString())).thenReturn(message("msgId"));

    engine.execute(workflow);
    engine.onEvent(messageReceived("/execute"));

    // wait for multiple executions: 1 with message, 2 by timer
    verify(messageService, timeout(5000).times(3)).send("abc", "Ok");
  }

}
