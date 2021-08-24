package com.symphony.bdk.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.swadl.WorkflowBuilder;
import com.symphony.bdk.workflow.swadl.exception.NoStartingEventException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class EngineIntegrationTest extends IntegrationTest {

  @Test
  void workflowWithoutStartCommand() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/no-start-command.swadl.yaml"));

    final V4Message message = message("msgId");
    final String streamId = "123";
    final String content = "<messageML>Hello!</messageML>";
    when(messageService.send(streamId, content)).thenReturn(message);

    assertThrows(NoStartingEventException.class,
        () -> engine.execute(workflow));
  }

  @Test
  void workflowWithSpaceInName() throws Exception {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/workflow-name-space.swadl.yaml"));

    engine.execute(workflow);
    engine.onEvent(messageReceived("/message"));

    verify(messageService, timeout(5000)).send(anyString(), any(Message.class));
  }

  @Test
  void stop() throws IOException, ProcessingException {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/message/send-message-on-message.swadl.yaml"));

    final V4Message message = message("msgId");
    final String streamId = "123";
    final String content = "<messageML>Hello!</messageML>";
    when(messageService.send(streamId, content)).thenReturn(message);

    engine.execute(workflow);
    engine.stop(workflow.getName());

    engine.onEvent(messageReceived("/message"));
    assertThat(lastProcess(workflow)).isEmpty();
  }
}
