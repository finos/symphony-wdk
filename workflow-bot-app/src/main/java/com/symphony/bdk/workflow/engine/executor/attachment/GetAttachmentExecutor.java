package com.symphony.bdk.workflow.engine.executor.attachment;

import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.gen.api.model.V4AttachmentInfo;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.attachment.GetAttachment;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
          String.format("No attachment in requested message with id %s", actualMessage.getMessageId()));
    }

    V4AttachmentInfo attachmentInfo = actualMessage.getAttachments().stream()
        .filter(attachment -> attachment.getId().equals(activity.getAttachmentId()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            String.format("No attachment with id %s found in message with id %s", activity.getAttachmentId(),
                activity.getMessageId())));


    String attachmentName = execution.getResourcesFolder() + attachmentInfo.getName();
    downloadAndStoreAttachment(attachmentName, actualMessage.getStream().getStreamId(), actualMessage.getMessageId(),
        attachmentInfo.getId(), execution.bdk().messages());

    execution.setOutputVariable(OUTPUT_ATTACHMENT_PATH_KEY, attachmentName);
  }

  private void downloadAndStoreAttachment(String attachmentName, String streamId, String messageId,
      String attachmentId, MessageService messages) {
    byte[] attachmentFromMessage = messages.getAttachment(streamId, messageId, attachmentId);
    byte[] decodedAttachmentFromMessage = Base64.getDecoder().decode(attachmentFromMessage);

    try {
      File file = new File(attachmentName);
      if (file.createNewFile()) {
        log.info("File {} has been created", attachmentName);
      } else {
        log.info("File {} already exist", attachmentName);
      }

      writeToFile(file, decodedAttachmentFromMessage, attachmentName);

    } catch (IOException exception) {
      log.debug("File {} creation failed", attachmentName, exception);
      throw new RuntimeException(exception);
    }
  }

  private void writeToFile(File file, byte[] content, String attachmentName) {
    try (FileOutputStream outputStream = new FileOutputStream(file)) {
      log.info("File {} content has been written", attachmentName);
      outputStream.write(content);
    } catch (Exception exception) {
      log.debug("File {} writing failed", attachmentName, exception);
      throw new RuntimeException(exception);
    }
  }


}
