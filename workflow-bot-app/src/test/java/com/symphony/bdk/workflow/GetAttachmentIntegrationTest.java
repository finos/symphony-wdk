package com.symphony.bdk.workflow;

import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@Slf4j
class GetAttachmentIntegrationTest extends IntegrationTest {

  private static final String OUTPUTS_ATTACHMENT_PATH_KEY = "%s.outputs.attachmentPath";

  @Test
  void getAttachment() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/attachments/get-attachment.swadl.yaml"));

    final V4Message actualMessage = createMessage("MSG_ID", "ATTACHMENT_ID", "myAttachment.txt");
    when(messageService.getMessage("MSG_ID")).thenReturn(actualMessage);
    when(messageService.getAttachment("STREAM_ID", "MSG_ID", "ATTACHMENT_ID")).thenReturn(mockBase64ByteArray());

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-attachment"));

    verify(messageService, timeout(5000).times(1)).getMessage("MSG_ID");
    verify(messageService, timeout(5000).times(1)).getAttachment("STREAM_ID", "MSG_ID", "ATTACHMENT_ID");

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUTS_ATTACHMENT_PATH_KEY, "getAttachment"),
            Paths.get("dummy", lastProcess(workflow).get(), "getAttachment-myAttachment.txt").toString());
  }

  @Test
  void getAttachmentNotFoundMessageId() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/attachments/get-attachment.swadl.yaml"));

    when(messageService.getMessage("MSG_ID")).thenReturn(null);
    engine.deploy(workflow);

    engine.onEvent(messageReceived("/get-attachment"));

    verify(messageService, timeout(5000).times(1)).getMessage("MSG_ID");
    verify(messageService, never()).getAttachment(anyString(), anyString(), anyString());
  }

  @Test
  void getAttachmentNotFoundAttachmentId() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/attachments/get-attachment.swadl.yaml"));

    // return a message without attachments
    when(messageService.getMessage("MSG_ID")).thenReturn(new V4Message());
    engine.deploy(workflow);

    engine.onEvent(messageReceived("/get-attachment"));

    verify(messageService, timeout(5000).times(1)).getMessage("MSG_ID");
    verify(messageService, never()).getAttachment(anyString(), anyString(), anyString());
  }

  @Test
  void getAttachmentBadAttachmentId() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/attachments/get-attachment.swadl.yaml"));

    final V4Message actualMessage = createMessage("MSG_ID", "FOUND_ATTACHMENT_ID", "myAttachment.txt");
    when(messageService.getMessage("MSG_ID")).thenReturn(actualMessage);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/get-attachment"));

    verify(messageService, timeout(5000).times(1)).getMessage("MSG_ID");
    verify(messageService, never()).getAttachment(anyString(), anyString(), anyString());
  }

}
