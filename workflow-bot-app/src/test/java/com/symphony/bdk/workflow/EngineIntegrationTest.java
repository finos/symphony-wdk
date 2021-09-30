package com.symphony.bdk.workflow;

import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.content;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.custom.assertion.Assertions;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.exception.NoStartingEventException;
import com.symphony.bdk.workflow.swadl.exception.SwadlNotValidException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class EngineIntegrationTest extends IntegrationTest {

  @Test
  void workflowWithoutStartCommand() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream("/no-start-command.swadl.yaml"));

    final V4Message message = message("msgId");
    final String streamId = "123";
    final String content = "<messageML>Hello!</messageML>";
    when(messageService.send(streamId, content)).thenReturn(message);

    assertThrows(NoStartingEventException.class,
        () -> engine.deploy(workflow));
  }

  @Test
  void workflowWithSpaceInName() {
    assertThrows(SwadlNotValidException.class,
        () -> SwadlParser.fromYaml(getClass().getResourceAsStream("/workflow-name-space.swadl.yaml")));
  }

  @Test
  void stop() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/send-message-on-message.swadl.yaml"));

    final V4Message message = message("msgId");
    final String streamId = "123";
    final String content = "<messageML>Hello!</messageML>";
    when(messageService.send(streamId, content)).thenReturn(message);

    engine.deploy(workflow);
    engine.undeploy(workflow.getId());

    engine.onEvent(messageReceived("/message"));
    assertThat(lastProcess(workflow)).isEmpty();
  }

  @Test
  void deployTwoWorkflowsSameId() throws IOException, ProcessingException {
    final Workflow workflowOne = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/deployment/deployment-different-workflow-same-id-1.swadl.yaml"));
    final Workflow workflowTwo = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/deployment/deployment-different-workflow-same-id-2.swadl.yaml"));

    when(messageService.send(anyString(), any(Message.class))).thenReturn(message("ignored message"));

    engine.deploy(workflowOne);
    engine.deploy(workflowTwo);

    engine.onEvent(messageReceived("/test"));

    verify(messageService, timeout(5000)).send(anyString(), content("message2"));
    verify(messageService, never()).send(anyString(), content("message1"));
    Assertions.assertThat(workflowTwo).isExecuted();
  }

}
