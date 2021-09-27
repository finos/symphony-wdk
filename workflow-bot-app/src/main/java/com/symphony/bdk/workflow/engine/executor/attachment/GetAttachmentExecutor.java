package com.symphony.bdk.workflow.engine.executor.attachment;

import com.symphony.bdk.gen.api.model.V4AttachmentInfo;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.attachment.GetAttachment;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;

@Slf4j
public class GetAttachmentExecutor implements ActivityExecutor<GetAttachment> {

  private static final String OUTPUT_ATTACHMENT_PATH_KEY = "attachmentPath";

  @Override
  public void execute(ActivityExecutorContext<GetAttachment> execution) throws IOException {
    GetAttachment activity = execution.getActivity();
    V4Message actualMessage = execution.bdk().messages().getMessage(activity.getMessageId());

    if (actualMessage == null) {
      throw new IllegalArgumentException(String.format("Message with id %s not found", activity.getMessageId()));
    }

    if (actualMessage.getAttachments() == null) {
      throw new IllegalStateException(
          String.format("No attachments in requested message with id %s", actualMessage.getMessageId()));
    }

    V4AttachmentInfo attachmentInfo = actualMessage.getAttachments().stream()
        .filter(attachment -> attachment.getId().equals(activity.getAttachmentId()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            String.format("No attachment with id %s found in message with id %s", activity.getAttachmentId(),
                activity.getMessageId())));

    byte[] attachmentFromMessage = execution.bdk().messages().getAttachment(
        actualMessage.getStream().getStreamId(), actualMessage.getMessageId(), attachmentInfo.getId());

    String fileName = String.format("%s-%s", execution.getCurrentActivityId(), attachmentInfo.getName());
    Path attachmentPath = storeAttachment(attachmentFromMessage, fileName, execution);

    execution.setOutputVariable(OUTPUT_ATTACHMENT_PATH_KEY, attachmentPath.toString());
  }

  private Path storeAttachment(byte[] attachmentFromMessage, String fileName,
      ActivityExecutorContext<GetAttachment> execution) throws IOException {

    byte[] decodedAttachmentFromMessage = Base64.getDecoder().decode(attachmentFromMessage);
    Path attachmentPath = Path.of(execution.getProcessInstanceId(), fileName);
    return execution.saveResource(attachmentPath, decodedAttachmentFromMessage);
  }

}
