package com.symphony.bdk.workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.custom.assertion.Assertions;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateMessageIntegrationTest extends IntegrationTest {

  private static final String OUTPUT_MESSAGE_ID_KEY = "%s.outputs.msgId";
  private static final String OUTPUT_MESSAGE_KEY = "%s.outputs.message";

  @Test
  void updateMessageSuccessfull() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/update-message.swadl.yaml"));
    final String msgId = "MSG_ID";
    final String content = "<messageML>Message Updated</messageML>";
    final V4Message message = message(msgId);
    final V4Message updatedMessage = message(msgId);

    when(messageService.getMessage(msgId)).thenReturn(message);
    when(messageService.update(any(V4Message.class), any(Message.class))).thenReturn(updatedMessage);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/update-valid-message"));

    ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
    verify(messageService, timeout(5000)).update(eq(message), messageArgumentCaptor.capture());

    assertThat(messageArgumentCaptor.getValue().getContent()).isEqualTo(content);
    Assertions.assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUT_MESSAGE_ID_KEY, "updateMessage"), msgId);
    Assertions.assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUT_MESSAGE_KEY, "updateMessage"), updatedMessage);

  }

  @Test
  void updateMessageFailed() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/update-message-invalid-id.swadl.yaml"));
    when(messageService.getMessage(eq("MSG_ID_INVALID"))).thenThrow(new RuntimeException("Failure"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/update-invalid-message"));

    verify(messageService, timeout(5000).times(0)).update(any(V4Message.class), any(Message.class));
  }
}
