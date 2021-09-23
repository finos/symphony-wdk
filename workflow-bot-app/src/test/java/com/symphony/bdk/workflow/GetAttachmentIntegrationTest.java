package com.symphony.bdk.workflow;

import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.gen.api.model.V4AttachmentInfo;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.gen.api.model.V4Stream;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;


@Slf4j
class GetAttachmentIntegrationTest extends IntegrationTest {

  private static final String OUTPUTS_ATTACHMENT_PATH_KEY = "%s.outputs.attachmentPath";

  @Test
  void getAttachment() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/attachments/get-attachment.swadl.yaml"));
    final String streamId = "STREAM_ID";
    final String msgId = "MSG_ID";
    final String attachmentId = "ATTACHMENT_ID";
    final String attachmentName = "/attachments/myAttachment.txt";
    final String expectedFilePath = Paths.get("src", "test", "resources").toFile().getAbsolutePath() + attachmentName;
    final byte[] encodedBytes = mockBase64ByteArray();
    final V4Message actualMessage = new V4Message();
    final V4Stream v4Stream = new V4Stream();
    final List<V4AttachmentInfo> attachments =
        Collections.singletonList(new V4AttachmentInfo().id(attachmentId).name(attachmentName));
    v4Stream.setStreamId("STREAM_ID");
    actualMessage.setMessageId(msgId);
    actualMessage.setStream(v4Stream);
    actualMessage.setAttachments(attachments);

    when(messageService.getMessage(msgId)).thenReturn(actualMessage);
    when(messageService.getAttachment(streamId, msgId, attachmentId)).thenReturn(encodedBytes);

    engine.deploy(workflow);

    engine.onEvent(messageReceived("/get-attachment"));

    verify(messageService, timeout(5000).times(1)).getMessage(msgId);
    verify(messageService, timeout(5000).times(1)).getAttachment(streamId, msgId, attachmentId);

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUTS_ATTACHMENT_PATH_KEY, "getattachment"), expectedFilePath);
  }

  @Test
  void getAttachmentNotFoundMessageId() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/attachments/get-attachment.swadl.yaml"));
    final String msgId = "MSG_ID";

    when(messageService.getMessage(msgId)).thenReturn(null);
    engine.deploy(workflow);

    engine.onEvent(messageReceived("/get-attachment"));

    verify(messageService, timeout(5000).times(1)).getMessage(msgId);
    verify(messageService, never()).getAttachment(anyString(), anyString(), anyString());
  }

  @Test
  void getAttachmentNotFoundAttachmentId() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/attachments/get-attachment.swadl.yaml"));
    final String msgId = "MSG_ID";

    when(messageService.getMessage(msgId)).thenReturn(new V4Message()); // return a message without attachments
    engine.deploy(workflow);

    engine.onEvent(messageReceived("/get-attachment"));

    verify(messageService, timeout(5000).times(1)).getMessage(msgId);
    verify(messageService, never()).getAttachment(anyString(), anyString(), anyString());
  }

  @Test
  void getAttachmentBadAttachmentId() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/attachments/get-attachment.swadl.yaml"));
    final String msgId = "MSG_ID";
    final String foundAttachmentId = "FOUND_ATTACHMENT_ID";
    final String attachmentName = "myAttachment.txt";
    final V4Message actualMessage = new V4Message();
    final V4Stream v4Stream = new V4Stream();
    final List<V4AttachmentInfo> attachments =
        Collections.singletonList(new V4AttachmentInfo().id(foundAttachmentId).name(attachmentName));
    v4Stream.setStreamId("STREAM_ID");
    actualMessage.setMessageId(msgId);
    actualMessage.setStream(v4Stream);
    actualMessage.setAttachments(attachments);

    when(messageService.getMessage(msgId)).thenReturn(actualMessage);

    engine.deploy(workflow);

    engine.onEvent(messageReceived("/get-attachment"));

    verify(messageService, timeout(5000).times(1)).getMessage(msgId);
    verify(messageService, never()).getAttachment(anyString(), anyString(), anyString());
  }

}
