package com.symphony.bdk.workflow;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.workflow.swadl.WorkflowBuilder;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class EventsIntegrationTest extends IntegrationTest {

  @Test
  void eventInTheMiddleOfWorkflow() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
            "/event/event-middle-workflow.swadl.yaml"));
    when(messageService.send(anyString(), anyString())).thenReturn(message("msgId"));

    engine.execute(workflow);
    engine.onEvent(messageReceived("abc", "/one"));
    verify(messageService, timeout(5000)).send("abc", "One");

    verify(messageService, never()).send("abc", "Two");
    engine.onEvent(messageReceived("abc", "/two"));
    verify(messageService, timeout(5000)).send("abc", "Two");
  }

  @Test
  void twoWorkflowsSameEvent() throws IOException, ProcessingException {
    final Workflow workflow1 = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
            "/event/msg1-event.swadl.yaml"));
    final Workflow workflow2 = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
            "/event/msg2-event.swadl.yaml"));
    when(messageService.send(anyString(), anyString())).thenReturn(message("msgId"));

    engine.execute(workflow1);
    engine.execute(workflow2);
    engine.onEvent(messageReceived("abc", "/msg"));

    verify(messageService, timeout(5000)).send("abc", "msg1");
    verify(messageService, timeout(5000)).send("abc", "msg2");
  }

  @Test
  void multipleStartingEvents() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
            "/event/multiple-events.swadl.yaml"));
    when(messageService.send(anyString(), anyString())).thenReturn(message("msgId"));

    engine.execute(workflow);

    engine.onEvent(messageReceived("abc", "/msg1"));
    engine.onEvent(messageReceived("abc", "/msg2"));

    verify(messageService, timeout(5000).times(2)).send("abc", "msg");
  }

  @Test
  void multipleEventsMiddleOfWorkflow() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
            "/event/mutiple-events-middle-workflow.swadl.yaml"));
    when(messageService.send(anyString(), anyString())).thenReturn(message("msgId"));

    engine.execute(workflow);

    // first execution
    engine.onEvent(messageReceived("abc", "/one"));
    engine.onEvent(messageReceived("abc", "/msg1"));

    // second execution
    engine.onEvent(messageReceived("abc", "/one"));
    engine.onEvent(messageReceived("abc", "/msg2"));

    verify(messageService, timeout(5000).times(2)).send("abc", "Two");
  }

  @Test
  void ifWithIntermediateEvent() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
            "/event/if-intermediate-event.swadl.yaml"));
    when(messageService.send(anyString(), anyString())).thenReturn(message("msgId"));

    engine.execute(workflow);

    engine.onEvent(messageReceived("abc", "/execute"));
    engine.onEvent(messageReceived("abc", "/execute2"));

    verify(messageService, timeout(5000)).send("abc", "act2");
  }

}
