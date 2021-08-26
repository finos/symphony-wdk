package com.symphony.bdk.workflow.engine.executor;

import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.core.service.stream.util.StreamUtil;
import com.symphony.bdk.gen.api.model.Stream;
import com.symphony.bdk.gen.api.model.V4AttachmentInfo;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.gen.api.model.V4SymphonyElementsAction;
import com.symphony.bdk.workflow.swadl.v1.activity.SendMessage;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class SendMessageExecutor implements ActivityExecutor<SendMessage> {

  private static final String OUTPUT_MESSAGE_ID_KEY = "msgId";

  @Override
  @SneakyThrows
  public void execute(ActivityExecutorContext<SendMessage> execution) {
    SendMessage activity = execution.getActivity();
    String streamId = resolveStreamId(execution, activity, execution.bdk().streams());
    log.debug("Sending message to room {}", streamId);

    if (streamId.endsWith("=")) { // TODO should be done in the BDK: https://perzoinc.atlassian.net/browse/PLAT-11214
      streamId = StreamUtil.toUrlSafeStreamId(streamId);
    }

    Message messageToSend = this.buildMessage(execution);

    V4Message message = execution.bdk().messages().send(streamId, messageToSend);

    execution.setOutputVariable(OUTPUT_MESSAGE_ID_KEY, message.getMessageId());
  }

  private String resolveStreamId(ActivityExecutorContext<SendMessage> execution, SendMessage activity,
      StreamService streamService) {
    if (activity.getTo() != null && activity.getTo().getStreamId() != null) {
      // either the stream id is set explicitly in the workflow
      return activity.getTo().getStreamId();

    } else if (activity.getTo() != null && activity.getTo().getUserIds() != null) {
      // or the user ids are set explicitly in the workflow
      return this.createOrGetStreamId(activity.getTo().getUserIds(), streamService);

    } else if (execution.getEvent() != null
        && execution.getEvent().getSource() instanceof V4MessageSent) {
      // or retrieved from the current even
      V4MessageSent event = (V4MessageSent) execution.getEvent().getSource();
      return event.getMessage().getStream().getStreamId();

    } else if (execution.getEvent() != null
        && execution.getEvent().getSource() instanceof V4SymphonyElementsAction) {
      V4SymphonyElementsAction event = (V4SymphonyElementsAction) execution.getEvent().getSource();
      return event.getStream().getStreamId();

    } else {
      throw new IllegalArgumentException("No stream id set to send a message");
    }
  }

  private String createOrGetStreamId(List<String> userIds, StreamService streamService) {
    List<Long> userIdsAsLong = userIds.stream().map(Long::parseLong).collect(Collectors.toList());
    Stream stream = streamService.create(userIdsAsLong);
    return stream.getId();
  }

  private Message buildMessage(ActivityExecutorContext<SendMessage> execution) throws IOException {
    Message.MessageBuilder builder = Message.builder().content(execution.getActivity().getContent());

    if (execution.getActivity().getAttachments() != null) {
      for (SendMessage.Attachment attachment : execution.getActivity().getAttachments()) {
        this.handleFileAttachment(builder, attachment, execution);
        this.handleForwardedAttachment(builder, attachment, execution.bdk().messages());
      }
    }

    return builder.build();
  }

  private void handleFileAttachment(Message.MessageBuilder messageBuilder, SendMessage.Attachment attachment,
      ActivityExecutorContext<SendMessage> execution) throws IOException {
    if (attachment.getContentPath() != null) {
      // stream is closed by HTTP client once the request body has been written
      InputStream content = this.loadAttachment(attachment.getContentPath(), execution);
      Path filename = Path.of(attachment.getContentPath()).getFileName();
      if (content != null && filename != null) {
        messageBuilder.addAttachment(content, filename.toString());
      }
    }
  }

  private void handleForwardedAttachment(Message.MessageBuilder messageBuilder, SendMessage.Attachment attachment,
      MessageService messages) {
    if (attachment.getMessageId() != null) {
      V4Message actualMessage = messages.getMessage(attachment.getMessageId());

      if (actualMessage == null) {
        throw new IllegalArgumentException(String.format("Message with id %s not found", attachment.getMessageId()));
      }

      if (attachment.getAttachmentId() != null) {
        // send the provided attachment only
        if (actualMessage.getAttachments() == null) {
          throw new IllegalStateException(
              String.format("No attachment in requested message with id %s", actualMessage.getMessageId()));
        }

        V4AttachmentInfo attachmentInfo = actualMessage.getAttachments().stream()
            .filter(a -> a.getId().equals(attachment.getAttachmentId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                String.format("No attachment with id %s found in message with id %s", attachment.getAttachmentId(),
                    attachment.getMessageId())));

        downloadAndAddAttachment(messageBuilder, actualMessage, attachmentInfo, messages);
      } else if (actualMessage.getAttachments() != null) {
        // send all message's attachments
        actualMessage.getAttachments()
            .forEach(a -> downloadAndAddAttachment(messageBuilder, actualMessage, a, messages));
      }
    }
  }

  private void downloadAndAddAttachment(Message.MessageBuilder messageBuilder,
      V4Message actualMessage, V4AttachmentInfo a, MessageService messages) {
    String filename = a.getName();
    byte[] attachmentFromMessage = messages
        .getAttachment(actualMessage.getStream().getStreamId(), actualMessage.getMessageId(), a.getId());
    byte[] decodedAttachmentFromMessage = Base64.getDecoder().decode(attachmentFromMessage);

    // stream is closed by HTTP client once the request body has been written (besides no need to close byte array)
    messageBuilder.addAttachment(new ByteArrayInputStream(decodedAttachmentFromMessage), filename);
  }

  private InputStream loadAttachment(String attachmentPath, ActivityExecutorContext<SendMessage> execution)
      throws IOException {
    if (attachmentPath == null) {
      return null;
    }

    return execution.getResource(attachmentPath);
  }

}
