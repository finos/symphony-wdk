package com.symphony.bdk.workflow;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.template.api.Template;
import com.symphony.bdk.template.api.TemplateEngine;
import com.symphony.bdk.workflow.custom.assertion.Assertions;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

public class UpdateMessageIntegrationTest extends IntegrationTest {

  private static final String OUTPUT_MESSAGE_ID_KEY = "%s.outputs.msgId";
  private static final String OUTPUT_MESSAGE_KEY = "%s.outputs.message";

  @ParameterizedTest
  @CsvSource({"update-message.swadl.yaml, true", "update-notsilent-message.swadl.yaml, false"})
  void updateMessageSuccessfully(String workflowFile, Boolean silent) throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/" + workflowFile));
    final String msgId = "MSG_ID";
    final String content = "<messageML>Message Updated</messageML>";
    final V4Message message = message(msgId);
    final V4Message updatedMessage = message(msgId);

    when(messageService.getMessage(msgId)).thenReturn(message);
    when(messageService.update(any(V4Message.class), any(Message.class))).thenReturn(updatedMessage);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/update-valid-message"));

    Assertions.assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUT_MESSAGE_ID_KEY, "updateMessage"), msgId)
        .hasOutput(String.format(OUTPUT_MESSAGE_KEY, "updateMessage"), updatedMessage);

    ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
    verify(messageService, times(1)).update(eq(message), messageArgumentCaptor.capture());
    assertThat(messageArgumentCaptor.getValue().getContent()).isEqualTo(content);
    assertThat(messageArgumentCaptor.getValue().getSilent()).isEqualTo(silent);
  }

  @Test
  void updateMessageWithTemplateSuccessfully() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/update-message-template.swadl.yaml"));
    final String msgId = "MSG_ID";
    final V4Message message = message(msgId);
    final V4Message updatedMessage = message(msgId);

    when(messageService.getMessage(msgId)).thenReturn(message);
    when(messageService.update(any(V4Message.class), any(Message.class))).thenReturn(updatedMessage);
    TemplateEngine templateEngine = mock(TemplateEngine.class);
    Template template = mock(Template.class);
    when(messageService.templates()).thenReturn(templateEngine);
    when(templateEngine.newTemplateFromFile(any())).thenReturn(template);
    when(template.process(any())).thenReturn("Hello with a template");

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/update"));

    Assertions.assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUT_MESSAGE_ID_KEY, "updateMessage"), msgId)
        .hasOutput(String.format(OUTPUT_MESSAGE_KEY, "updateMessage"), updatedMessage);

    ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
    verify(messageService, times(1)).update(eq(message), messageArgumentCaptor.capture());
    assertThat(messageArgumentCaptor.getValue().getContent()).isEqualTo("<messageML>Hello with a template</messageML>");
  }

  @Test
  void updateMessageFailed() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/update-message-invalid-id.swadl.yaml"));
    when(messageService.getMessage(eq("MSG_ID_INVALID"))).thenThrow(new RuntimeException("Failure"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/update-invalid-message"));

    Assertions.assertThat(workflow).isExecuted();
    verify(messageService, times(0)).update(any(V4Message.class), any(Message.class));
  }
}
