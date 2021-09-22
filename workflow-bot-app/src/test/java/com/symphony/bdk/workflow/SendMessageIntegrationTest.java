package com.symphony.bdk.workflow;

import static com.symphony.bdk.workflow.custom.assertion.Assertions.assertThat;
import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.assertMessage;
import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.content;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.service.message.model.Attachment;
import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.Stream;
import com.symphony.bdk.gen.api.model.V4AttachmentInfo;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.gen.api.model.V4Stream;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

class SendMessageIntegrationTest extends IntegrationTest {

  private static final String OUTPUTS_MSG_KEY = "%s.outputs.message";

  @Test
  @DisplayName(
      "Given a send-message with a streamId, when the triggering message is received, "
          + "then the provided message should be sent to the room")
  void sendMessageOnMessage() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/send-message-on-message.swadl.yaml"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/message"));

    verify(messageService, timeout(5000)).send(eq("123"), content("Hello!"));
  }

  @Test
  @DisplayName(
      "Given two activities: create-room and send-message, when the room is created, then a message is sent to it")
  void sendMessageToCreatedRoomOnMessage() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/room/create-room-and-send-message.swadl.yaml"));
    final List<Long> uids = Arrays.asList(1234L, 5678L);
    final Stream stream = new Stream().id("0000");

    when(streamService.create(uids)).thenReturn(stream);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/create-room"));

    verify(streamService, timeout(5000).times(1)).create(uids);
    verify(messageService, timeout(5000).times(1)).send(anyString(), content("<p>Hello!</p>"));
  }

  @Test
  @DisplayName(
      "Given a list of user ids, when the workflow is executed, then a message is sent to their IM/MIM"
  )
  void sendMessageWithUids() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/send-message-with-uids.swadl.yaml"));

    final List<Long> uids = Arrays.asList(123L, 456L);
    final String streamId = "STREAM_ID";
    final String msgId = "MSG_ID";
    final String content = "<messageML>hello</messageML>";
    final V4Message message = message(msgId);

    when(streamService.create(uids)).thenReturn(stream(streamId));
    when(messageService.send(eq(streamId), any(Message.class))).thenReturn(message);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/send"));

    ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<List<Long>> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);

    verify(streamService, timeout(5000).times(1)).create(listArgumentCaptor.capture());
    verify(messageService, timeout(5000).times(1)).send(stringArgumentCaptor.capture(),
        messageArgumentCaptor.capture());

    assertThat(listArgumentCaptor.getAllValues().size()).as("The create method is called with a list as parameter")
        .isEqualTo(1);
    assertThat(listArgumentCaptor.getAllValues().get(0)).isEqualTo(uids);
    assertThat(stringArgumentCaptor.getValue()).isEqualTo(streamId);
    assertThat(messageArgumentCaptor.getValue().getContent()).isEqualTo(content);

    assertThat(workflow).isExecuted().hasOutput(String.format(OUTPUTS_MSG_KEY, "sendMessageWithUserIds"), message);
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
    final List<V4AttachmentInfo> attachments = new ArrayList<>();
    attachments.add(new V4AttachmentInfo().id("ATTACHMENT_ID").name(attachmentFilename));
    final V4Message actualMessage = new V4Message();
    final V4Stream v4Stream = new V4Stream();
    v4Stream.setStreamId("STREAM_ID");
    actualMessage.setMessageId("MSG_ID");
    actualMessage.setStream(v4Stream);
    actualMessage.setAttachments(attachments);

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
    final List<V4AttachmentInfo> attachments = new ArrayList<>();
    attachments.add(new V4AttachmentInfo().id("ATTACHMENT_ID_1").name(attachmentFilename1));
    attachments.add(new V4AttachmentInfo().id("ATTACHMENT_ID_2").name(attachmentFilename2));
    final V4Message actualMessage = new V4Message();
    final V4Stream v4Stream = new V4Stream();
    v4Stream.setStreamId("STREAM_ID");
    actualMessage.setMessageId("MSG_ID");
    actualMessage.setStream(v4Stream);
    actualMessage.setAttachments(attachments);

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
    final List<V4AttachmentInfo> attachments = new ArrayList<>();
    attachments.add(new V4AttachmentInfo().id("ATTACHMENT_ID_1").name(attachmentFilename1));
    attachments.add(new V4AttachmentInfo().id("ATTACHMENT_ID_2").name(attachmentFilename2));
    final V4Message actualMessage = new V4Message();
    final V4Stream v4Stream = new V4Stream();
    v4Stream.setStreamId("STREAM_ID");
    actualMessage.setMessageId("MSG_ID");
    actualMessage.setStream(v4Stream);
    actualMessage.setAttachments(attachments);

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

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/send-attachment-from-file"));

    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
    verify(messageService, timeout(5000)).send(anyString(), argumentCaptor.capture());

    Message messageSent = argumentCaptor.getValue();
    assertThat(messageSent).as("A non null message has been sent").isNotNull();
    assertThat(messageSent.getContent()).as("The sent message has the correct content").isEqualTo(content);
    assertThat(messageSent.getAttachments().size()).as("The sent message has 2 attachments").isEqualTo(2);
  }

  @Test
  void sendBlastMessage() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/send-blast-message.swadl.yaml"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/send-blast"));

    verify(messageService, timeout(5000)).send(eq(List.of("ABC", "DEF")), any());
  }

}
