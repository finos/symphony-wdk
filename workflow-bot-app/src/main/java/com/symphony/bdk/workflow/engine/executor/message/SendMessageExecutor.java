package com.symphony.bdk.workflow.engine.executor.message;

import static java.util.Collections.singletonList;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.message.OboMessageService;
import com.symphony.bdk.core.service.message.exception.PresentationMLParserException;
import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.gen.api.model.V4AttachmentInfo;
import com.symphony.bdk.gen.api.model.V4ConnectionAccepted;
import com.symphony.bdk.gen.api.model.V4ConnectionRequested;
import com.symphony.bdk.gen.api.model.V4InstantMessageCreated;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.gen.api.model.V4MessageBlastResponse;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.gen.api.model.V4MessageSuppressed;
import com.symphony.bdk.gen.api.model.V4RoomCreated;
import com.symphony.bdk.gen.api.model.V4RoomDeactivated;
import com.symphony.bdk.gen.api.model.V4RoomMemberDemotedFromOwner;
import com.symphony.bdk.gen.api.model.V4RoomMemberPromotedToOwner;
import com.symphony.bdk.gen.api.model.V4RoomReactivated;
import com.symphony.bdk.gen.api.model.V4RoomUpdated;
import com.symphony.bdk.gen.api.model.V4SharedPost;
import com.symphony.bdk.gen.api.model.V4SymphonyElementsAction;
import com.symphony.bdk.gen.api.model.V4UserJoinedRoom;
import com.symphony.bdk.gen.api.model.V4UserLeftRoom;
import com.symphony.bdk.gen.api.model.V4UserRequestedToJoinRoom;
import com.symphony.bdk.http.api.ApiRuntimeException;
import com.symphony.bdk.workflow.engine.camunda.UtilityFunctionsMapper;
import com.symphony.bdk.workflow.engine.camunda.WorkflowEventToCamundaEvent;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.EventHolder;
import com.symphony.bdk.workflow.engine.executor.obo.OboExecutor;
import com.symphony.bdk.workflow.swadl.v1.Event;
import com.symphony.bdk.workflow.swadl.v1.EventResolver;
import com.symphony.bdk.workflow.swadl.v1.EventWithTimeout;
import com.symphony.bdk.workflow.swadl.v1.activity.message.SendMessage;
import com.symphony.bdk.workflow.swadl.v1.event.ConnectionAcceptedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.ConnectionRequestedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.ImCreatedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.MessageSuppressedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.PostSharedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RequestReceivedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RoomDeactivatedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RoomMemberDemotedFromOwnerEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RoomMemberPromotedToOwnerEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RoomReactivatedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RoomUpdatedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.UserJoinedRoomEvent;
import com.symphony.bdk.workflow.swadl.v1.event.UserLeftRoomEvent;
import com.symphony.bdk.workflow.swadl.v1.event.UserRequestedToJoinRoomEvent;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class SendMessageExecutor extends OboExecutor<SendMessage, V4Message>
    implements ActivityExecutor<SendMessage> {

  // required for message correlation and forms (correlation happens on variables than cannot be nested)
  public static final String OUTPUT_MESSAGE_ID_KEY = "msgId";
  public static final String OUTPUT_MESSAGE_KEY = "message";
  public static final String OUTPUT_MESSAGES_KEY = "messages";
  public static final String OUTPUT_FAILED_MESSAGES_KEY = "failedStreamIds";

  @Override
  public void execute(ActivityExecutorContext<SendMessage> execution)
      throws IOException, PresentationMLParserException {
    log.debug("Sending message...");
    SendMessage activity = execution.getActivity();
    List<String> streamIds = resolveStreamId(execution, activity, execution.bdk().streams());
    log.debug("Sending message to rooms {}", streamIds);
    Message messageToSend = this.buildMessage(execution);
    log.trace("message content \n {}", messageToSend.getContent());

    V4Message message;
    List<V4Message> messages = new ArrayList<>();
    List<String> failedStreamIds = new ArrayList<>();

    if (streamIds.isEmpty()) {
      throw new IllegalArgumentException(
          String.format("No stream/user ids set to send a message in activity %s", activity.getId()));

    } else if (isObo(activity) && activity.getObo() != null && streamIds.size() == 1) {
      message = this.doOboWithCache(execution);

    } else if (isObo(activity) && streamIds.size() > 1) {
      // TODO: Add blast message obo case when it is enabled: https://perzoinc.atlassian.net/browse/PLAT-11231
      throw new IllegalArgumentException(
          String.format("Blast message, in activity %s, is not OBO enabled", activity.getId()));

    } else if (streamIds.size() == 1) {
      message = execution.bdk().messages().send(streamIds.get(0), messageToSend);

    } else {
      V4MessageBlastResponse response = execution.bdk().messages().send(streamIds, messageToSend);

      if (response.getMessages() != null && !response.getMessages().isEmpty()) {
        message = response.getMessages().get(0); // for backward compatibility, we keep storing the first message
        messages.addAll(response.getMessages());
      } else {
        throw new RuntimeException(String.format("All messages have failed in activity %s", activity.getId()));
      }

      if (response.getErrors() != null) {
        failedStreamIds.addAll(response.getErrors().keySet());
      }
    }

    Map<String, Object> outputs = new HashMap<>();
    outputs.put(OUTPUT_MESSAGE_KEY, message);
    outputs.put(OUTPUT_MESSAGE_ID_KEY, message != null ? message.getMessageId() : null);
    outputs.put(OUTPUT_MESSAGES_KEY, messages);
    outputs.put(OUTPUT_FAILED_MESSAGES_KEY, failedStreamIds);
    execution.setOutputVariables(outputs);
  }

  @Override
  protected V4Message doOboWithCache(ActivityExecutorContext<SendMessage> execution)
      throws IOException, PresentationMLParserException {
    SendMessage activity = execution.getActivity();
    List<String> streamIds = resolveStreamId(execution, activity, execution.bdk().streams());

    if (streamIds.isEmpty()) {
      throw new IllegalArgumentException(
          String.format("No stream ids set to send a message in activity %s", activity.getId()));
    }

    String streamId = streamIds.get(0);
    Message messageToSend = this.buildMessage(execution);
    AuthSession authSession = this.getOboAuthSession(execution);

    OboMessageService messages = execution.bdk()
        .obo(authSession)
        .messages();

    return messages.send(streamId, messageToSend);
  }

  /*private String getMessageReceivedEventContent(SendMessage activity) {
    if (activity.getOn() == null) {
      return null;
    }

    if (activity.getOn().getMessageReceived() != null) {
      return activity.getOn().getMessageReceived().getContent();
    }

    if (activity.getOn().getOneOf() != null) {
      Optional<Event> optional = activity.getOn().getOneOf()
          .stream()
          .filter(event -> event.getMessageReceived() != null && StringUtils.isNotBlank(
              event.getMessageReceived().getContent()))
          .findFirst();

      if (optional.isPresent()) {
        return optional.get().getMessageReceived().getContent();
      }
    }

    if (activity.getOn().getAllOf() != null) {
      Optional<Event> optional = activity.getOn().getAllOf()
          .stream()
          .filter(event -> event.getMessageReceived() != null && StringUtils.isNotBlank(
              event.getMessageReceived().getContent()))
          .findFirst();

      if (optional.isPresent()) {
        return optional.get().getMessageReceived().getContent();
      }
    }

    return null;
  }*/

  private String getFormElementActionEventFormId(SendMessage activity) {
    if (activity.getOn() == null) {
      return null;
    }

    if (activity.getOn().getFormReplied() != null) {
      return activity.getOn().getFormReplied().getFormId();
    }

    if (activity.getOn().getOneOf() != null) {
      Optional<Event> optional = activity.getOn().getOneOf()
          .stream()
          .filter(event -> event.getFormReplied() != null && StringUtils.isNotBlank(event.getFormReplied().getFormId()))
          .findFirst();

      if (optional.isPresent()) {
        return optional.get().getFormReplied().getFormId();
      }
    }

    if (activity.getOn().getAllOf() != null) {
      Optional<Event> optional = activity.getOn().getAllOf()
          .stream()
          .filter(event -> event.getFormReplied() != null && StringUtils.isNotBlank(event.getFormReplied().getFormId()))
          .findFirst();

      if (optional.isPresent()) {
        return optional.get().getFormReplied().getFormId();
      }
    }

    return null;
  }

  private EventHolder<Object> resolveEvent(ActivityExecutorContext<SendMessage> execution)
      throws PresentationMLParserException {
    Map<String, EventHolder<Object>> events = execution.getEvents();
    EventResolver eventResolver = new EventResolver(execution.getActivity().getOn());

    EventHolder<Object> eventHolder = null;

    String messageReceivedEventContent = eventResolver.getMessageReceivedEventContent();
    if (messageReceivedEventContent != null) {
      eventHolder = events.get(WorkflowEventToCamundaEvent.getUniqueName(
          new V4MessageSent().message(new V4Message().message(messageReceivedEventContent.replace("/", "")))));
    }

    String formRepliedEventFormId = this.getFormElementActionEventFormId(execution.getActivity());
    if (formRepliedEventFormId != null) {
      eventHolder = events.get(
          WorkflowEventToCamundaEvent.getUniqueName(new V4SymphonyElementsAction().formId(formRepliedEventFormId)));
    }

    EventWithTimeout on = execution.getActivity().getOn();
    if (on == null) {
      return eventHolder;
    }

    /*if (on.isEventOfType(ImCreatedEvent.class.getSimpleName())) {
      eventHolder = events.get(WorkflowEventToCamundaEvent.getUniqueName(new RoomCreatedEvent()));
    }*/

    if (eventResolver.isEventOfType(ImCreatedEvent.class.getSimpleName())) {
      eventHolder = events.get(WorkflowEventToCamundaEvent.getUniqueName(new V4RoomCreated()));
    }

    if (eventResolver.isEventOfType((RoomUpdatedEvent.class.getSimpleName()))) {
      eventHolder = events.get(WorkflowEventToCamundaEvent.getUniqueName(new V4RoomUpdated()));
    }

    if (eventResolver.isEventOfType((RoomDeactivatedEvent.class.getSimpleName()))) {
      eventHolder = events.get(WorkflowEventToCamundaEvent.getUniqueName(new V4RoomDeactivated()));
    }

    if (eventResolver.isEventOfType((RoomReactivatedEvent.class.getSimpleName()))) {
      eventHolder = events.get(WorkflowEventToCamundaEvent.getUniqueName(new V4RoomReactivated()));
    }

    if (eventResolver.isEventOfType((UserJoinedRoomEvent.class.getSimpleName()))) {
      eventHolder = events.get(WorkflowEventToCamundaEvent.getUniqueName(new V4UserJoinedRoom()));
    }

    if (eventResolver.isEventOfType((UserLeftRoomEvent.class.getSimpleName()))) {
      eventHolder = events.get(WorkflowEventToCamundaEvent.getUniqueName(new V4UserLeftRoom()));
    }

    if (eventResolver.isEventOfType((RoomMemberDemotedFromOwnerEvent.class.getSimpleName()))) {
      eventHolder = events.get(WorkflowEventToCamundaEvent.getUniqueName(new V4RoomMemberDemotedFromOwner()));
    }

    if (eventResolver.isEventOfType((RoomMemberPromotedToOwnerEvent.class.getSimpleName()))) {
      eventHolder = events.get(WorkflowEventToCamundaEvent.getUniqueName(new V4RoomMemberPromotedToOwner()));
    }

    if (eventResolver.isEventOfType((MessageSuppressedEvent.class.getSimpleName()))) {
      eventHolder = events.get(WorkflowEventToCamundaEvent.getUniqueName(new V4MessageSuppressed()));
    }

    if (eventResolver.isEventOfType((PostSharedEvent.class.getSimpleName()))) {
      eventHolder = events.get(WorkflowEventToCamundaEvent.getUniqueName(new V4SharedPost()));
    }

    if (eventResolver.isEventOfType((ImCreatedEvent.class.getSimpleName()))) {
      eventHolder = events.get(WorkflowEventToCamundaEvent.getUniqueName(new V4InstantMessageCreated()));
    }

    if (eventResolver.isEventOfType((UserRequestedToJoinRoomEvent.class.getSimpleName()))) {
      eventHolder = events.get(WorkflowEventToCamundaEvent.getUniqueName(new V4UserRequestedToJoinRoom()));
    }

    if (eventResolver.isEventOfType((ConnectionRequestedEvent.class.getSimpleName()))) {
      eventHolder = events.get(WorkflowEventToCamundaEvent.getUniqueName(new V4ConnectionRequested()));
    }

    if (eventResolver.isEventOfType((ConnectionAcceptedEvent.class.getSimpleName()))) {
      eventHolder = events.get(WorkflowEventToCamundaEvent.getUniqueName(new V4ConnectionAccepted()));
    }

    if (eventResolver.isEventOfType((RequestReceivedEvent.class.getSimpleName()))) {
      eventHolder = events.get(WorkflowEventToCamundaEvent.getUniqueName(new RequestReceivedEvent()));
    }

    return eventHolder;
  }

  private List<String> resolveStreamId(ActivityExecutorContext<SendMessage> execution, SendMessage activity,
      StreamService streamService) throws PresentationMLParserException {
    EventHolder<Object> eventHolder = resolveEvent(execution);

    if (activity.getTo() != null && activity.getTo().getStreamId() != null) {
      // either the stream id is set explicitly in the workflow
      return singletonList(activity.getTo().getStreamId());
    } else if (activity.getTo() != null && activity.getTo().getStreamIds() != null) {
      return activity.getTo().getStreamIds();
    } else if (activity.getTo() != null && activity.getTo().getUserIds() != null) {
      // or the user ids are set explicitly in the workflow
      return this.createOrGetStreamId(activity.getTo().getUserIds(), streamService);
    } else if (eventHolder != null && eventHolder.getSource() instanceof V4MessageSent) {
      // or retrieved from the current event
      V4MessageSent event = (V4MessageSent) eventHolder.getSource();
      return singletonList(event.getMessage().getStream().getStreamId());
    } else if (eventHolder != null && eventHolder.getSource() instanceof V4SymphonyElementsAction) {
      V4SymphonyElementsAction event = (V4SymphonyElementsAction) eventHolder.getSource();
      return singletonList(event.getStream().getStreamId());
    } else if (eventHolder != null
        && eventHolder.getSource() instanceof V4UserJoinedRoom) {
      V4UserJoinedRoom event = (V4UserJoinedRoom) eventHolder.getSource();
      return singletonList(event.getStream().getStreamId());
    } else if (eventHolder != null
        && eventHolder.getSource() instanceof V4RoomUpdated) {
      V4RoomUpdated event = (V4RoomUpdated) eventHolder.getSource();
      return singletonList(event.getStream().getStreamId());
    } else if (eventHolder != null
        && eventHolder.getSource() instanceof V4RoomMemberPromotedToOwner) {
      V4RoomMemberPromotedToOwner event = (V4RoomMemberPromotedToOwner) eventHolder.getSource();
      return singletonList(event.getStream().getStreamId());
    } else {
      throw new IllegalArgumentException(
          String.format("No stream id set to send a message in activity %s", activity.getId()));
    }
  }

  private List<String> createOrGetStreamId(List<Long> userIds, StreamService streamService) {
    List<String> streamIds = new ArrayList<>();

    for (Long userId : userIds) {
      // passing a singleton list of long instead of long to make the test mocking easy
      try {
        streamIds.add(streamService.create(List.of(userId)).getId());
      } catch (ApiRuntimeException apiRuntimeException) {
        // ignore error when user is not found
        if (apiRuntimeException.getCode() != 403) {
          throw apiRuntimeException;
        }
      }
    }

    return streamIds;
  }

  private Message buildMessage(ActivityExecutorContext<SendMessage> execution) throws IOException {
    Message.MessageBuilder builder = Message.builder().content(extractContent(execution));
    if (StringUtils.isNotBlank(execution.getActivity().getData())) {
      builder.data(execution.getActivity().getData());
    }
    if (execution.getActivity().getAttachments() != null) {
      for (SendMessage.Attachment attachment : execution.getActivity().getAttachments()) {
        this.handleFileAttachment(builder, attachment, execution);
        this.handleForwardedAttachment(builder, attachment, execution.bdk().messages());
      }
    }

    return builder.build();
  }

  private static String extractContent(ActivityExecutorContext<SendMessage> execution) throws IOException {
    if (execution.getActivity().getContent() != null) {
      return execution.getActivity().getContent();
    } else {
      String template = execution.getActivity().getTemplate();
      File file = execution.getResourceFile(Path.of(template));
      Map<String, Object> templateVariables = new HashMap<>(execution.getVariables());
      // also bind our utility functions so they can be used inside templates
      templateVariables.put(UtilityFunctionsMapper.NAME, new UtilityFunctionsMapper());
      return execution.bdk()
          .messages()
          .templates()
          .newTemplateFromFile(file.getPath())
          .process(templateVariables);
    }
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

    return execution.getResource(Path.of(attachmentPath));
  }

}
