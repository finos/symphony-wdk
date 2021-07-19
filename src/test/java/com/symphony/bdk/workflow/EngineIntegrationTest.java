package com.symphony.bdk.workflow;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.lang.WorkflowBuilder;
import com.symphony.bdk.workflow.lang.swadl.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.camunda.bpm.engine.MismatchingMessageCorrelationException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class EngineIntegrationTest extends IntegrationTest {

  @Test
  void stop() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/send-message-on-message.yaml"));
    final V4Message message = new V4Message();
    message.setMessageId("msgId");

    final String streamId = "123";
    final String content = "<messageML>Hello!</messageML>";
    when(messageService.send(streamId, content)).thenReturn(message);

    engine.execute(workflow);
    engine.stop(workflow.getName());

    assertThrows(MismatchingMessageCorrelationException.class,
        () -> engine.messageReceived("123", "/message"));
  }
}
