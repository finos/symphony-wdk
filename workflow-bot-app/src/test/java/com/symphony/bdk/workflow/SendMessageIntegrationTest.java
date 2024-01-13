package com.symphony.bdk.workflow;

import com.symphony.bdk.core.service.message.model.Attachment;
import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.Error;
import com.symphony.bdk.gen.api.model.Stream;
import com.symphony.bdk.gen.api.model.V4AttachmentInfo;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.gen.api.model.V4MessageBlastResponse;
import com.symphony.bdk.gen.api.model.V4Stream;
import com.symphony.bdk.http.api.ApiException;
import com.symphony.bdk.http.api.ApiRuntimeException;
import com.symphony.bdk.template.api.TemplateEngine;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.symphony.bdk.workflow.custom.assertion.Assertions.assertThat;
import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.assertMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class SendMessageIntegrationTest extends IntegrationTest {

  private static final String OUTPUTS_MSG_KEY = "%s.outputs.message";
  private static final String OUTPUTS_MSG_ID_KEY = "%s.outputs.msgId";
  private static final String OUTPUTS_MESSAGES_KEY = "%s.outputs.messages";
  private static final String OUTPUTS_FAILED_MESSAGES_KEY = "%s.outputs.failedStreamIds";

  @Test
  void sendMessageOnMessage() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/send-message-on-message.swadl.yaml"));
    final V4Message message = message("Hello!");

    when(messageService.send(anyString(), any(Message.class))).thenReturn(message);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/message"));

    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(messageService, timeout(5000)).send(anyString(), captor.capture());

    assertThat(captor.getValue().getData()).contains("id", "123456");
    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUTS_MSG_KEY, "sendMessage1"), message)
        .hasOutput(String.format(OUTPUTS_MSG_ID_KEY, "sendMessage1"), message.getMessageId())
        .hasOutput(String.format(OUTPUTS_MESSAGES_KEY, "sendMessage1"), Collections.EMPTY_LIST)
        .hasOutput(String.format(OUTPUTS_FAILED_MESSAGES_KEY, "sendMessage1"), Collections.EMPTY_LIST);
  }

  @Test
  void sendMessageToCreatedRoomOnMessage() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/room/create-room-and-send-message.swadl.yaml"));
    final List<Long> uids = Arrays.asList(1234L, 5678L);
    final Stream stream = new Stream().id("0000");

    final V4Message message = message("Hello!");

    when(streamService.create(uids)).thenReturn(stream);
    when(messageService.send(anyString(), any(Message.class))).thenReturn(message);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/create-room-and-send-msg"));

    verify(streamService, timeout(5000).times(1)).create(uids);
    verify(messageService, timeout(5000).times(1)).send(anyString(), any(Message.class));

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUTS_MSG_KEY, "sendmessageid"), message)
        .hasOutput(String.format(OUTPUTS_MSG_ID_KEY, "sendmessageid"), message.getMessageId())
        .hasOutput(String.format(OUTPUTS_MESSAGES_KEY, "sendmessageid"), Collections.EMPTY_LIST)
        .hasOutput(String.format(OUTPUTS_FAILED_MESSAGES_KEY, "sendmessageid"), Collections.EMPTY_LIST);
  }

  @Test
  void sendMessageWithOneUid() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/send-message-with-user-id.swadl.yaml"));
    final String content = "<messageML>hello</messageML>";
    final V4Message message = message(content);

    when(streamService.create(List.of(123L))).thenReturn(stream("streamId1"));
    when(messageService.send(eq("streamId1"), any(Message.class))).thenReturn(message);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/send-msg-with-userid"));

    ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
    ArgumentCaptor<List<Long>> userIdCaptor = ArgumentCaptor.forClass(List.class);

    verify(streamService, timeout(5000)).create(userIdCaptor.capture());
    verify(messageService, timeout(5000)).send(anyString(), messageCaptor.capture());

    assertThat(userIdCaptor.getAllValues()).hasSize(1);
    assertThat(userIdCaptor.getValue().get(0)).isEqualTo(123L);
    assertThat(messageCaptor.getValue().getContent()).isEqualTo(content);

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUTS_MSG_KEY, "sendMessageWithUserId"), message)
        .hasOutput(String.format(OUTPUTS_MSG_ID_KEY, "sendMessageWithUserId"), message.getMessageId())
        .hasOutput(String.format(OUTPUTS_MESSAGES_KEY, "sendMessageWithUserId"), Collections.EMPTY_LIST)
        .hasOutput(String.format(OUTPUTS_FAILED_MESSAGES_KEY, "sendMessageWithUserId"), Collections.EMPTY_LIST);
  }

  @ParameterizedTest
  @CsvSource({"403", "405"})
  void sendMessageWithInvalidUid(String errorCode) throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/send-message-with-user-id.swadl.yaml"));

    when(streamService.create(List.of(123L))).thenThrow(
        new ApiRuntimeException(new ApiException(Integer.parseInt(errorCode), "Invalid user id")));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/send-msg-with-userid"));

    ArgumentCaptor<List<Long>> userIdCaptor = ArgumentCaptor.forClass(List.class);

    verify(streamService, timeout(5000)).create(userIdCaptor.capture());
    verify(messageService, never()).send(anyString(), any(Message.class));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/message/send-message-with-freemarker.swadl.yaml",
      "/message/send-message-with-inline-template.swadl.yaml"})
  void sendMessageWithTemplateSuccessful(String swadl) throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream(swadl));

    final V4Message message = message("MSG_ID");
    final TemplateEngine templateEngine = TemplateEngine.getDefaultImplementation();

    when(messageService.templates()).thenReturn(templateEngine);
    when(messageService.send(eq("123"), any(Message.class))).thenReturn(message);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/send-with-freemarker"));

    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(messageService, timeout(5000).times(1)).send(eq("123"), captor.capture());

    assertThat(captor.getValue().getContent())
        .isEqualTo("<messageML>Hello world!\n</messageML>");
    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUTS_MSG_KEY, "sendMessageTemplateWithParams"), message)
        .hasOutput(String.format(OUTPUTS_MSG_ID_KEY, "sendMessageTemplateWithParams"), message.getMessageId())
        .hasOutput(String.format(OUTPUTS_MESSAGES_KEY, "sendMessageTemplateWithParams"), Collections.EMPTY_LIST)
        .hasOutput(String.format(OUTPUTS_FAILED_MESSAGES_KEY, "sendMessageTemplateWithParams"), Collections.EMPTY_LIST);
  }

  @Test
  @DisplayName(
      "Given a message with attachments, "
          + "when I send a new message with the attachment id, "
          + "this specific file is sent in a new message")
  void sendMessageWithSpecificAttachment() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream(
            "/message/forward-specific-attachment-in-message.swadl.yaml"));
    final V4Message messageToReturn = message("msgId");

    final String streamId = "123";
    final String messageId = "MSG_ID";
    final String content = "<messageML>here is a msg with attachment</messageML>";
    final String attachmentFilename = "filename.png";
    final byte[] encodedBytes = mockBase64ByteArray();
    final byte[] decodedBytes = Base64.getDecoder().decode(encodedBytes);
    final List<V4AttachmentInfo> attachments =
        Collections.singletonList(new V4AttachmentInfo().id("ATTACHMENT_ID").name(attachmentFilename));
    final V4Stream v4Stream = new V4Stream().streamId("STREAM_ID");
    final V4Message actualMessage = new V4Message().messageId("MSG_ID").stream(v4Stream).attachments(attachments);

    when(messageService.getMessage(messageId)).thenReturn(actualMessage);
    when(messageService.getAttachment("STREAM_ID", "MSG_ID", "ATTACHMENT_ID")).thenReturn(encodedBytes);
    when(messageService.send(eq(streamId), any(Message.class))).thenReturn(messageToReturn);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/forward-specific"));

    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
    verify(messageService, timeout(5000)).send(eq(streamId), argumentCaptor.capture());

    Message expectedMessage = this.buildMessage(content,
        Collections.singletonList(new Attachment(new ByteArrayInputStream(decodedBytes), attachmentFilename)));

    assertMessage(argumentCaptor.getValue(), expectedMessage);

    assertThat(workflow).executed("forwardProvidedAttachmentInMessageId")
        .hasOutput(String.format(OUTPUTS_MSG_KEY, "forwardProvidedAttachmentInMessageId"), messageToReturn)
        .hasOutput(String.format(OUTPUTS_MSG_ID_KEY, "forwardProvidedAttachmentInMessageId"),
            messageToReturn.getMessageId())
        .hasOutput(String.format(OUTPUTS_MESSAGES_KEY, "forwardProvidedAttachmentInMessageId"), Collections.EMPTY_LIST)
        .hasOutput(String.format(OUTPUTS_FAILED_MESSAGES_KEY, "forwardProvidedAttachmentInMessageId"),
            Collections.EMPTY_LIST);
  }

  @Test
  @DisplayName(
      "Given a message without attachments, when I send a new message with the attachment id, the message is not sent")
  void sendMessageWithUnfoundMessage() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream(
            "/message/forward-unfound-message-in-message.swadl.yaml"));
    final String messageId = "MSG_ID";

    when(messageService.getMessage(messageId)).thenReturn(new V4Message());

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/forward-unfound-message"));

    verify(messageService, never()).send(anyString(), any(Message.class));

    assertThat(workflow).executed("forwardUnfoundMessage").notExecuted("scriptActivityNotToBeExecuted");
  }

  @Test
  @DisplayName(
      "Given an unfound message, when I send a new message with the attachment id, the message is not sent")
  void sendMessageWithUnfoundAttachment() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream(
            "/message/forward-unfound-attachment-in-message.swadl.yaml"));
    final String messageId = "MSG_ID";

    when(messageService.getMessage(messageId)).thenReturn(null);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/forward-unfound-attachment"));

    verify(messageService, never()).send(anyString(), any(Message.class));

    assertThat(workflow).executed("forwardUnfoundAttachment").notExecuted("scriptActivityNotToBeExecuted");
  }

  @Test
  @DisplayName(
      "Given a message with attachments, when I send a new message with multiple attachment ids,"
          + "then only theses attachments are sent in a new message")
  void sendMessageWithMultipleAttachments() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream(
            "/message/forward-multiple-attachments-in-message.swadl.yaml"));
    final V4Message messageToReturn = message("msgId");

    final String streamId = "123";
    final String messageId = "MSG_ID";
    final String content = "<messageML>here is a msg with attachment</messageML>";
    final String attachmentFilename1 = "filename1.png";
    final String attachmentFilename2 = "filename2.png";
    final byte[] encodedBytes = mockBase64ByteArray();
    final byte[] decodedBytes = Base64.getDecoder().decode(encodedBytes);
    final List<V4AttachmentInfo> attachments =
        Arrays.asList(new V4AttachmentInfo().id("ATTACHMENT_ID_1").name(attachmentFilename1),
            new V4AttachmentInfo().id("ATTACHMENT_ID_2").name(attachmentFilename2));
    final V4Stream v4Stream = new V4Stream().streamId("STREAM_ID");
    final V4Message actualMessage = new V4Message().messageId("MSG_ID").stream(v4Stream).attachments(attachments);

    when(messageService.getMessage(messageId)).thenReturn(actualMessage);
    when(messageService.getAttachment("STREAM_ID", "MSG_ID", "ATTACHMENT_ID_1")).thenReturn(encodedBytes);
    when(messageService.getAttachment("STREAM_ID", "MSG_ID", "ATTACHMENT_ID_2")).thenReturn(encodedBytes);
    when(messageService.send(eq(streamId), any(Message.class))).thenReturn(messageToReturn);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/forward-multiple"));

    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
    verify(messageService, timeout(5000)).send(eq(streamId), argumentCaptor.capture());

    Message expectedMessage = this.buildMessage(content,
        Arrays.asList(new Attachment(new ByteArrayInputStream(decodedBytes), attachmentFilename1),
            new Attachment(new ByteArrayInputStream(decodedBytes), attachmentFilename2)));

    assertMessage(argumentCaptor.getValue(), expectedMessage);
    assertThat(workflow).executed("forwardMultiple")
        .hasOutput(String.format(OUTPUTS_MSG_KEY, "forwardMultiple"), messageToReturn)
        .hasOutput(String.format(OUTPUTS_MSG_ID_KEY, "forwardMultiple"),
            messageToReturn.getMessageId())
        .hasOutput(String.format(OUTPUTS_MESSAGES_KEY, "forwardMultiple"), Collections.EMPTY_LIST)
        .hasOutput(String.format(OUTPUTS_FAILED_MESSAGES_KEY, "forwardMultiple"), Collections.EMPTY_LIST);
  }

  @Test
  @DisplayName(
      "Given a message with attachments, when I send a new message without any attachment id,"
          + "then all message's attachments are sent in a new message")
  void sendMessageWithAllAttachments() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/forward-all-attachments-in-message.swadl.yaml"));
    final V4Message messageToReturn = message("msgId");

    final String streamId = "123";
    final String messageId = "MSG_ID";
    final String content = "<messageML>here is a msg with attachment</messageML>";
    final String attachmentFilename1 = "filename1.png";
    final String attachmentFilename2 = "filename2.png";
    final byte[] encodedBytes = mockBase64ByteArray();
    final byte[] decodedBytes = Base64.getDecoder().decode(encodedBytes);
    final List<V4AttachmentInfo> attachments =
        Arrays.asList(new V4AttachmentInfo().id("ATTACHMENT_ID_1").name(attachmentFilename1),
            new V4AttachmentInfo().id("ATTACHMENT_ID_2").name(attachmentFilename2));
    final V4Stream v4Stream = new V4Stream().streamId("STREAM_ID");
    final V4Message actualMessage = new V4Message().messageId("MSG_ID").stream(v4Stream).attachments(attachments);

    when(messageService.getMessage(messageId)).thenReturn(actualMessage);
    when(messageService.getAttachment("STREAM_ID", "MSG_ID", "ATTACHMENT_ID_1")).thenReturn(encodedBytes);
    when(messageService.getAttachment("STREAM_ID", "MSG_ID", "ATTACHMENT_ID_2")).thenReturn(encodedBytes);
    when(messageService.send(eq(streamId), any(Message.class))).thenReturn(messageToReturn);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/forward-all"));

    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
    verify(messageService, timeout(5000)).send(eq(streamId), argumentCaptor.capture());

    Message expectedMessage = this.buildMessage(content,
        Arrays.asList(new Attachment(new ByteArrayInputStream(decodedBytes), attachmentFilename1),
            new Attachment(new ByteArrayInputStream(decodedBytes), attachmentFilename2)));

    assertMessage(argumentCaptor.getValue(), expectedMessage);
    assertThat(workflow).executed("forwardAll")
        .hasOutput(String.format(OUTPUTS_MSG_KEY, "forwardAll"), messageToReturn)
        .hasOutput(String.format(OUTPUTS_MSG_ID_KEY, "forwardAll"),
            messageToReturn.getMessageId())
        .hasOutput(String.format(OUTPUTS_MESSAGES_KEY, "forwardAll"), Collections.EMPTY_LIST)
        .hasOutput(String.format(OUTPUTS_FAILED_MESSAGES_KEY, "forwardAll"), Collections.EMPTY_LIST);
  }

  @Test
  @DisplayName(
      "Given a local stored file, when I send a new message with the file path,"
          + "then the file is sent as attachment in a new message")
  void sendMessageWithAttachmentsFromFile() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(
            getClass().getResourceAsStream("/message/send-attachments-from-file-in-message.swadl.yaml"));
    final String content = "<messageML>here is a msg with attachment</messageML>";
    final V4Message messageToReturn = message("MSG_WITH_ATTACHMENT_ID");

    when(messageService.send(eq("123"), any(Message.class))).thenReturn(messageToReturn);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/send-attachment-from-file"));

    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
    verify(messageService, timeout(5000)).send(anyString(), argumentCaptor.capture());

    Message messageSent = argumentCaptor.getValue();
    assertThat(messageSent).as("A non null message has been sent").isNotNull();
    assertThat(messageSent.getContent()).as("The sent message has the correct content").isEqualTo(content);
    assertThat(messageSent.getAttachments()).as("The sent message has 2 attachments").hasSize(2);

    assertThat(workflow).executed("sendAttachmentFromFileInMessageId")
        .hasOutput(String.format(OUTPUTS_MSG_KEY, "sendAttachmentFromFileInMessageId"), messageToReturn)
        .hasOutput(String.format(OUTPUTS_MSG_ID_KEY, "sendAttachmentFromFileInMessageId"),
            messageToReturn.getMessageId())
        .hasOutput(String.format(OUTPUTS_MESSAGES_KEY, "sendAttachmentFromFileInMessageId"), Collections.EMPTY_LIST)
        .hasOutput(String.format(OUTPUTS_FAILED_MESSAGES_KEY, "sendAttachmentFromFileInMessageId"),
            Collections.EMPTY_LIST);
  }

  @Test
  void sendBlastMessageWithStreamIdsAllSuccessful() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/send-blast-message-with-stream-ids.swadl.yaml"));
    final V4Message message = new V4Message().messageId("MSG_ID");
    V4MessageBlastResponse response = new V4MessageBlastResponse().messages(Collections.singletonList(message));

    when(messageService.send(eq(List.of("ABC", "DEF")), any())).thenReturn(response);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("ABC", "/send-blast", "MSG_ID"));

    verify(messageService, timeout(5000)).send(eq(List.of("ABC", "DEF")), any());

    assertThat(workflow).executed("sendBlastMessageWithStreamIds")
        .hasOutput(String.format(OUTPUTS_MSG_KEY, "sendBlastMessageWithStreamIds"), message)
        .hasOutput(String.format(OUTPUTS_MSG_ID_KEY, "sendBlastMessageWithStreamIds"), message.getMessageId())
        .hasOutput(String.format(OUTPUTS_MESSAGES_KEY, "sendBlastMessageWithStreamIds"),
            Collections.singletonList(message))
        .hasOutput(String.format(OUTPUTS_FAILED_MESSAGES_KEY, "sendBlastMessageWithStreamIds"), Collections.EMPTY_LIST);
  }

  @Test
  void sendBlastMessageWithStreamIdsAllFailing() throws Exception {
    final Workflow workflow = SwadlParser.fromYaml(
        getClass().getResourceAsStream("/message/send-blast-message-with-stream-ids-all-failing.swadl.yaml"));

    final Map<String, Error> errors = Map.of("ABC", new Error().code(403), "DEF", new Error().code(403));
    final V4MessageBlastResponse response = new V4MessageBlastResponse().errors(errors);

    when(messageService.send(eq(List.of("ABC", "DEF")), any(Message.class))).thenReturn(response);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/send-blast"));

    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);

    verify(messageService, timeout(5000)).send(eq(List.of("ABC", "DEF")), captor.capture());

    assertThat(captor.getValue().getContent()).isEqualTo("<messageML>hello</messageML>");
    assertThat(workflow).executed("sendBlastMessageWithStreamIdsAllFailing")
        .notExecuted("scriptActivityNotToBeExecuted");
  }

  @Test
  void sendBlastMessageWithStreamIdsOneSuccessfulOneFailing() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/send-blast-message-with-stream-ids.swadl.yaml"));

    final V4Message successfulMessage = new V4Message()
        .stream(new V4Stream().streamId("ABC"))
        .message("<messageML>hello</messageML>")
        .messageId("MSG_ID");
    final Map<String, Error> errors = Map.of("DEF", new Error().code(403));
    final V4MessageBlastResponse response =
        new V4MessageBlastResponse().errors(errors).messages(Collections.singletonList(successfulMessage));

    when(messageService.send(eq(List.of("ABC", "DEF")), any())).thenReturn(response);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("ABC", "/send-blast", "MSG_ID"));

    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);

    verify(messageService, timeout(5000)).send(eq(List.of("ABC", "DEF")), captor.capture());

    assertThat(captor.getValue().getContent()).isEqualTo("<messageML>hello</messageML>");
    assertThat(workflow).executed("sendBlastMessageWithStreamIds")
        .hasOutput(String.format(OUTPUTS_MSG_KEY, "sendBlastMessageWithStreamIds"), successfulMessage)
        .hasOutput(String.format(OUTPUTS_MSG_ID_KEY, "sendBlastMessageWithStreamIds"),
            successfulMessage.getMessageId())
        .hasOutput(String.format(OUTPUTS_MESSAGES_KEY, "sendBlastMessageWithStreamIds"),
            Collections.singletonList(successfulMessage))
        .hasOutput(String.format(OUTPUTS_FAILED_MESSAGES_KEY, "sendBlastMessageWithStreamIds"),
            Collections.singletonList("DEF"));
  }

  @Test
  void sendBlastMessageWithUidsAllSuccessful() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/send-blast-message-with-uids.swadl.yaml"));

    final String streamId1 = "STREAM_ID_1";
    final String streamId2 = "STREAM_ID_2";
    final String msgId = "MSG_ID";
    final String content = "<messageML>hello</messageML>";
    final V4Message message = message(msgId);
    final V4MessageBlastResponse response = new V4MessageBlastResponse().addMessagesItem(message);

    when(streamService.create(List.of(123L))).thenReturn(stream(streamId1));
    when(streamService.create(List.of(456L))).thenReturn(stream(streamId2));
    when(messageService.send(eq(List.of(streamId1, streamId2)), any(Message.class))).thenReturn(response);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/send"));

    ArgumentCaptor<List<Long>> userIdsCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<List<String>> streamIdsArgumentCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);

    verify(streamService, timeout(5000).times(2)).create(userIdsCaptor.capture());
    verify(messageService, timeout(5000).times(1)).send(streamIdsArgumentCaptor.capture(),
        messageArgumentCaptor.capture());

    assertThat(userIdsCaptor.getAllValues()).as("One create method is called by user id").hasSize(2);
    assertThat(userIdsCaptor.getAllValues().get(0).get(0)).isEqualTo(123L);
    assertThat(userIdsCaptor.getAllValues().get(1).get(0)).isEqualTo(456L);

    assertThat(streamIdsArgumentCaptor.getValue()).hasSameElementsAs(List.of(streamId1, streamId2));
    assertThat(messageArgumentCaptor.getValue().getContent()).isEqualTo(content);

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUTS_MSG_KEY, "sendBlastMessageWithUserIds"), message)
        .hasOutput(String.format(OUTPUTS_MSG_ID_KEY, "sendBlastMessageWithUserIds"), message.getMessageId())
        .hasOutput(String.format(OUTPUTS_MESSAGES_KEY, "sendBlastMessageWithUserIds"),
            Collections.singletonList(message))
        .hasOutput(String.format(OUTPUTS_FAILED_MESSAGES_KEY, "sendBlastMessageWithUserIds"), Collections.EMPTY_LIST);
  }

  @Test
  void sendBlastMessageWithUidsOneSuccessfulOneFailing() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/send-blast-message-with-uids.swadl.yaml"));

    final String streamId1 = "STREAM_ID_1";
    final String streamId2 = "STREAM_ID_2";
    final String msgId = "MSG_ID";
    final String content = "<messageML>hello</messageML>";
    final V4Message message = message(msgId);
    final Map<String, Error> errors = Map.of(streamId2, new Error().code(403));
    final V4MessageBlastResponse response =
        new V4MessageBlastResponse().errors(errors).messages(Collections.singletonList(message));

    when(streamService.create(List.of(123L))).thenReturn(stream(streamId1));
    when(streamService.create(List.of(456L))).thenReturn(stream(streamId2));
    when(messageService.send(eq(List.of(streamId1, streamId2)), any(Message.class))).thenReturn(response);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/send"));

    ArgumentCaptor<List<Long>> userIdsCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<List<String>> streamIdsArgumentCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);

    verify(streamService, timeout(5000).times(2)).create(userIdsCaptor.capture());
    verify(messageService, timeout(5000).times(1)).send(streamIdsArgumentCaptor.capture(),
        messageArgumentCaptor.capture());

    assertThat(userIdsCaptor.getAllValues()).as("One create method is called by user id").hasSize(2);
    assertThat(userIdsCaptor.getAllValues().get(0).get(0)).isEqualTo(123L);
    assertThat(userIdsCaptor.getAllValues().get(1).get(0)).isEqualTo(456L);

    assertThat(streamIdsArgumentCaptor.getValue()).hasSameElementsAs(List.of(streamId1, streamId2));
    assertThat(messageArgumentCaptor.getValue().getContent()).isEqualTo(content);

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUTS_MSG_KEY, "sendBlastMessageWithUserIds"), message)
        .hasOutput(String.format(OUTPUTS_MSG_ID_KEY, "sendBlastMessageWithUserIds"), message.getMessageId())
        .hasOutput(String.format(OUTPUTS_MESSAGES_KEY, "sendBlastMessageWithUserIds"),
            Collections.singletonList(message))
        .hasOutput(String.format(OUTPUTS_FAILED_MESSAGES_KEY, "sendBlastMessageWithUserIds"),
            Collections.singletonList(streamId2));
  }

  @Test
  void sendBlastMessageWithUidsAllFailing() throws Exception {
    final Workflow workflow = SwadlParser.fromYaml(
        getClass().getResourceAsStream("/message/send-blast-message-with-uids-all-failing.swadl.yaml"));

    final String streamId1 = "STREAM_ID_1";
    final String streamId2 = "STREAM_ID_2";
    final Map<String, Error> errors = Map.of(streamId1, new Error().code(403), streamId2, new Error().code(403));
    final V4MessageBlastResponse response = new V4MessageBlastResponse().errors(errors);

    when(streamService.create(List.of(123L))).thenReturn(stream(streamId1));
    when(streamService.create(List.of(456L))).thenReturn(stream(streamId2));
    when(messageService.send(eq(List.of(streamId1, streamId2)), any(Message.class))).thenReturn(response);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/send"));

    ArgumentCaptor<List<Long>> userIdsCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<List<String>> streamIdsArgumentCaptor = ArgumentCaptor.forClass(List.class);

    verify(streamService, timeout(5000).times(2)).create(userIdsCaptor.capture());
    verify(messageService, timeout(5000).times(1)).send(streamIdsArgumentCaptor.capture(), any());

    assertThat(userIdsCaptor.getAllValues()).as("One create method is called by user id").hasSize(2);
    assertThat(userIdsCaptor.getAllValues().get(0).get(0)).isEqualTo(123L);
    assertThat(userIdsCaptor.getAllValues().get(1).get(0)).isEqualTo(456L);

    assertThat(streamIdsArgumentCaptor.getValue()).hasSameElementsAs(List.of(streamId1, streamId2));

    assertThat(workflow).executed("sendBlastMessageWithUserIdsAllFailing").notExecuted("scriptActivityNotToBeExecuted");
  }

  @Test
  void sendBlastMessageWithUidsVariables() throws Exception {
    final Workflow workflow = SwadlParser.fromYaml(
        getClass().getResourceAsStream("/message/send-message-blast-with-uids-variables.swadl.yaml"));

    final List<Long> uids = List.of(123L);
    final String streamId = "STREAM_ID";
    final String msgId = "MSG_ID";
    final String content = "<messageML>hello</messageML>";
    final V4Message message = message(msgId);

    when(streamService.create(uids)).thenReturn(stream(streamId));
    when(messageService.send(eq(streamId), any(Message.class))).thenReturn(message);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/send-with-variables"));

    ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<List<Long>> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);

    verify(streamService, timeout(5000).times(1)).create(listArgumentCaptor.capture());
    verify(messageService, timeout(5000).times(1)).send(stringArgumentCaptor.capture(),
        messageArgumentCaptor.capture());

    assertThat(listArgumentCaptor.getAllValues()).as("The create method is called with a list as parameter").hasSize(1);
    assertThat(listArgumentCaptor.getAllValues().get(0)).isEqualTo(uids);
    assertThat(stringArgumentCaptor.getValue()).isEqualTo(streamId);
    assertThat(messageArgumentCaptor.getValue().getContent()).isEqualTo(content);

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUTS_MSG_KEY, "sendBlastMessageWithUserIds"), message)
        .hasOutput(String.format(OUTPUTS_MSG_ID_KEY, "sendBlastMessageWithUserIds"), message.getMessageId())
        .hasOutput(String.format(OUTPUTS_MESSAGES_KEY, "sendBlastMessageWithUserIds"), Collections.EMPTY_LIST)
        .hasOutput(String.format(OUTPUTS_FAILED_MESSAGES_KEY, "sendBlastMessageWithUserIds"), Collections.EMPTY_LIST);
  }

  @ParameterizedTest
  @CsvSource({"/message/obo/send-message-obo-valid-username.swadl.yaml, /message-obo-valid-username",
      "/message/obo/send-message-obo-valid-userid.swadl.yaml, /message-obo-valid-userid"})
  void sendMessageObo(String workflowFile, String command) throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream(workflowFile));
    final V4Message message = message("Hello!");

    when(oboMessageService.send(anyString(), any(Message.class))).thenReturn(message);
    when(bdkGateway.obo(any(String.class))).thenReturn(botSession);
    when(bdkGateway.obo(any(Long.class))).thenReturn(botSession);

    engine.deploy(workflow);
    engine.onEvent(messageReceived(command));

    verify(oboMessageService, timeout(5000)).send(anyString(), any(Message.class));

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUTS_MSG_KEY, "sendMessageObo"), message)
        .hasOutput(String.format(OUTPUTS_MSG_ID_KEY, "sendMessageObo"), message.getMessageId())
        .hasOutput(String.format(OUTPUTS_MESSAGES_KEY, "sendMessageObo"), Collections.EMPTY_LIST)
        .hasOutput(String.format(OUTPUTS_FAILED_MESSAGES_KEY, "sendMessageObo"), Collections.EMPTY_LIST);
  }

  @Test
  void sendMessageOboUnauthorized() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/obo/send-message-obo-unauthorized.swadl.yaml"));

    when(bdkGateway.obo(any(Long.class))).thenThrow(new RuntimeException("Unauthorized user"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/message-obo-unauthorized"));

    assertThat(workflow).executed("sendMessageObo")
        .notExecuted("scriptActivityNotToBeExecuted");
  }

  @ParameterizedTest
  @CsvSource({"/message/obo/send-blast-message-obo-not-supported.swadl.yaml",
      "/message/obo/send-blast-message-with-userids-obo-not-supported.swadl.yaml"})
  void sendBlastMessageOboNotSupported(String workflowFile) throws Exception {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(workflowFile));

    when(bdkGateway.obo(any(String.class))).thenReturn(botSession);
    when(bdkGateway.obo(any(Long.class))).thenReturn(botSession);

    when(streamService.create(List.of(123L))).thenReturn(stream("streamId1"));
    when(streamService.create(List.of(456L))).thenReturn(stream("streamId2"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/message-not-supported"));

    assertThat(workflow).executed("sendBlastMessageObo")
        .notExecuted("scriptActivityNotToBeExecuted");
  }

}
