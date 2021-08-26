package com.symphony.bdk.workflow;

import static com.symphony.bdk.workflow.custom.assertion.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.service.pagination.model.PaginationAttribute;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.swadl.WorkflowBuilder;
import com.symphony.bdk.workflow.swadl.exception.SwadlNotValidException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

class GetMessagesIntegrationTest extends IntegrationTest {

  private final String OUTPUTS_ONE_MESSAGE_KEY = "%s.outputs.message";
  private final String OUTPUTS_LIST_MESSAGES_KEY = "%s.outputs.messages";

  @Test
  @DisplayName("Given a message with an id, when the workflow is triggered, then the message is returned")
  void getMessageByIdFound() throws Exception {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/message/get-message-by-id.swadl.yaml"));
    final String msgId = "MSG_ID";

    when(messageService.getMessage(msgId)).thenReturn(message(msgId));

    engine.execute(workflow);
    engine.onEvent(messageReceived("/get-msg-by-id-found"));

    verify(messageService, timeout(5000)).getMessage(msgId);


    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUTS_ONE_MESSAGE_KEY, "getMessageByIdFound"), message(msgId));
  }

  @Test
  @DisplayName("Given a message id that does not exist, when the workflow is triggered, then an exception is thrown")
  void getMessageByIdUnfound() throws Exception {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/message/get-message-by-id-unfound.swadl.yaml"));
    final String msgId = "MSG_ID";

    when(messageService.getMessage(msgId)).thenReturn(null);

    engine.execute(workflow);
    engine.onEvent(messageReceived("/get-msg-by-id-unfound"));

    verify(messageService, timeout(5000)).getMessage(msgId);

    assertThat(workflow)
        .isExecuted()
        .hasOutput(String.format(OUTPUTS_ONE_MESSAGE_KEY, "getMessageByIdUnfound"), null);
  }

  @Test
  @DisplayName("Given a streamId, when the workflow is triggered, then stream's messages are sent with pagination")
  void getMessageByStreamIdWithPagination() throws Exception {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(
            getClass().getResourceAsStream("/message/get-message-by-stream-id-with-pagination.swadl.yaml"));
    final String streamId = "STREAM_ID";

    List<V4Message> messages = Arrays.asList(message("MSG1"), message("MSG2"));

    when(messageService.listMessages(eq(streamId), any(Instant.class), any(PaginationAttribute.class))).thenReturn(
        messages);

    engine.execute(workflow);
    engine.onEvent(messageReceived("/get-msg-by-stream-id-with-pagination"));

    ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Instant> instantArgumentCaptor = ArgumentCaptor.forClass(Instant.class);
    ArgumentCaptor<PaginationAttribute> paginationArgumentCaptor = ArgumentCaptor.forClass(PaginationAttribute.class);
    verify(messageService, timeout(5000)).listMessages(stringArgumentCaptor.capture(), instantArgumentCaptor.capture(),
        paginationArgumentCaptor.capture());

    assertEquals(stringArgumentCaptor.getValue(), streamId);
    assertNotNull(instantArgumentCaptor.getValue());
    PaginationAttribute pagination = paginationArgumentCaptor.getValue();
    assertEquals(pagination.getLimit(), 2);
    assertEquals(pagination.getSkip(), 0);

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUTS_LIST_MESSAGES_KEY, "listMessagesWithPagination"), messages);
  }

  @Test
  @DisplayName(
      "Given a streamId, when the workflow is triggered without pagination, then stream's messages are all sent")
  void getMessageByStreamIdWithoutPagination() throws Exception {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(
            getClass().getResourceAsStream("/message/get-message-by-stream-id-without-pagination.swadl.yaml"));
    final String streamId = "STREAM_ID";

    List<V4Message> messages = Arrays.asList(message("MSG1"), message("MSG2"));

    when(messageService.listMessages(eq(streamId), any(Instant.class))).thenReturn(messages);

    engine.execute(workflow);
    engine.onEvent(messageReceived("/get-msg-by-stream-id-without-pagination"));

    ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Instant> instantArgumentCaptor = ArgumentCaptor.forClass(Instant.class);
    verify(messageService, timeout(5000)).listMessages(stringArgumentCaptor.capture(), instantArgumentCaptor.capture());

    assertThat(stringArgumentCaptor.getValue()).isEqualTo(streamId);
    assertThat(instantArgumentCaptor.getValue()).isNotNull();
    assertThat(workflow)
        .isExecuted()
        .hasOutput(String.format(OUTPUTS_LIST_MESSAGES_KEY, "listMessagesWithoutPagination"), messages);
  }

  @Test
  @DisplayName(
      "Given a get message by streamid without since parameter, when the workflow is triggered,"
          + "then an error is thrown")
  void getMessageByStreamIdWithoutSince_invalidWorkflow() {
    assertThatThrownBy(() -> WorkflowBuilder.fromYaml(
        getClass().getResourceAsStream("/message/get-message-by-stream-id-invalid.swadl.yaml"))).isInstanceOf(
        SwadlNotValidException.class);
  }
}
