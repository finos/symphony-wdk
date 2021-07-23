package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.core.service.message.exception.PresentationMLParserException;
import com.symphony.bdk.core.service.message.util.PresentationMLParser;
import com.symphony.bdk.gen.api.model.V4ConnectionAccepted;
import com.symphony.bdk.gen.api.model.V4ConnectionRequested;
import com.symphony.bdk.gen.api.model.V4InstantMessageCreated;
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
import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.EventHolder;
import com.symphony.bdk.workflow.lang.swadl.Event;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// There might be a way to make this make this more generic/less code but waiting for
// event filtering to see how it is going to evolve, at least it is easy to understand.
@Component
public class EventToMessage {

  static final String MESSAGE_PREFIX = "message-received_";
  static final String MESSAGE_SUPPRESSED = "message-suppressed";
  static final String POST_SHARED = "post-shared";
  static final String IM_CREATED = "im-created";
  static final String FORM_REPLY_PREFIX = "formReply_";
  static final String ROOM_CREATED = "room-created";
  static final String ROOM_UPDATED = "room-updated";
  static final String ROOM_DEACTIVATED = "room-deactivated";
  static final String ROOM_REACTIVATED = "room-reactivated";
  static final String ROOM_MEMBER_PROMOTED_TO_OWNER = "room-member-promoted-to-owner-event";
  static final String ROOM_MEMBER_DEMOTED_FROM_OWNER = "room-member-demoted-from-owner-event";
  static final String USER_REQUESTED_JOIN_ROOM = "user-requested-join-room";
  static final String USER_JOINED_ROOM = "user-joined-room";
  static final String USER_LEFT_ROOM = "user-left-room";
  static final String CONNECTION_REQUESTED = "connection-requested";
  static final String CONNECTION_ACCEPTED = "connection-accepted";

  @Autowired
  private RuntimeService runtimeService;

  public static Optional<String> toMessageName(Event event) {
    if (event.getMessageReceived() != null) {
      return Optional.of(MESSAGE_PREFIX + event.getMessageReceived().getContent());

    } else if (event.getMessageSuppressed() != null) {
      return Optional.of(MESSAGE_SUPPRESSED);

    } else if (event.getImCreated() != null) {
      return Optional.of(IM_CREATED);

    } else if (event.getPostShared() != null) {
      return Optional.of(POST_SHARED);

    } else if (event.getRoomCreated() != null) {
      return Optional.of(ROOM_CREATED);

    } else if (event.getRoomUpdated() != null) {
      return Optional.of(ROOM_UPDATED);

    } else if (event.getRoomDeactivated() != null) {
      return Optional.of(ROOM_DEACTIVATED);

    } else if (event.getRoomReactived() != null) {
      return Optional.of(ROOM_REACTIVATED);

    } else if (event.getRoomMemberPromotedToOwner() != null) {
      return Optional.of(ROOM_MEMBER_PROMOTED_TO_OWNER);

    } else if (event.getRoomMemberDemotedFromOwner() != null) {
      return Optional.of(ROOM_MEMBER_DEMOTED_FROM_OWNER);

    } else if (event.getUserRequestedJoinRoom() != null) {
      return Optional.of(USER_REQUESTED_JOIN_ROOM);

    } else if (event.getUserJoinedRoom() != null) {
      return Optional.of(USER_JOINED_ROOM);

    } else if (event.getUserLeftRoom() != null) {
      return Optional.of(USER_LEFT_ROOM);

    } else if (event.getConnectionRequested() != null) {
      return Optional.of(CONNECTION_REQUESTED);

    } else if (event.getConnectionAccepted() != null) {
      return Optional.of(CONNECTION_ACCEPTED);
    }

    return Optional.empty();
  }

  public <T> MessageCorrelationBuilder toMessage(RealTimeEvent<T> event)
      throws PresentationMLParserException {
    MessageCorrelationBuilder messageCorrelation = null;

    Map<String, Object> processVariables = new HashMap<>();
    processVariables.put(ActivityExecutorContext.EVENT, new EventHolder<>(event.getInitiator(), event.getSource()));

    if (event.getSource() instanceof V4SymphonyElementsAction) {
      // we expect the activity id to be the same as the form id to work
      // correlation across processes is based on the message id tha was created to send the form
      V4SymphonyElementsAction implEvent = (V4SymphonyElementsAction) event.getSource();
      Map<String, Object> formReplies = (Map<String, Object>) implEvent.getFormValues();
      String formId = implEvent.getFormId();
      processVariables.put(formId, formReplies);
      messageCorrelation = runtimeService.createMessageCorrelation(EventToMessage.FORM_REPLY_PREFIX + formId)
          .processInstanceVariableEquals(formId + ".outputs.msgId", implEvent.getFormMessageId())
          .setVariables(processVariables);

    } else if (event.getSource() instanceof V4MessageSent) {
      RealTimeEvent<V4MessageSent> implEvent = (RealTimeEvent<V4MessageSent>) event;
      String presentationMl = implEvent.getSource().getMessage().getMessage();
      String textContent = PresentationMLParser.getTextContent(presentationMl);

      messageCorrelation = runtimeService.createMessageCorrelation(EventToMessage.MESSAGE_PREFIX + textContent)
          .setVariables(processVariables);

    } else if (event.getSource() instanceof V4RoomCreated) {
      messageCorrelation = runtimeService.createMessageCorrelation(EventToMessage.ROOM_CREATED)
          .setVariables(processVariables);

    } else if (event.getSource() instanceof V4RoomUpdated) {
      messageCorrelation = runtimeService.createMessageCorrelation(EventToMessage.ROOM_UPDATED)
          .setVariables(processVariables);

    } else if (event.getSource() instanceof V4RoomDeactivated) {
      messageCorrelation = runtimeService.createMessageCorrelation(EventToMessage.ROOM_DEACTIVATED)
          .setVariables(processVariables);

    } else if (event.getSource() instanceof V4RoomReactivated) {
      messageCorrelation = runtimeService.createMessageCorrelation(EventToMessage.ROOM_REACTIVATED)
          .setVariables(processVariables);

    } else if (event.getSource() instanceof V4UserJoinedRoom) {
      messageCorrelation = runtimeService.createMessageCorrelation(EventToMessage.USER_JOINED_ROOM)
          .setVariables(processVariables);

    } else if (event.getSource() instanceof V4UserLeftRoom) {
      messageCorrelation = runtimeService.createMessageCorrelation(EventToMessage.USER_LEFT_ROOM)
          .setVariables(processVariables);

    } else if (event.getSource() instanceof V4RoomMemberDemotedFromOwner) {
      messageCorrelation = runtimeService.createMessageCorrelation(EventToMessage.ROOM_MEMBER_DEMOTED_FROM_OWNER)
          .setVariables(processVariables);

    } else if (event.getSource() instanceof V4RoomMemberPromotedToOwner) {
      messageCorrelation = runtimeService.createMessageCorrelation(EventToMessage.ROOM_MEMBER_PROMOTED_TO_OWNER)
          .setVariables(processVariables);

    } else if (event.getSource() instanceof V4MessageSuppressed) {
      messageCorrelation = runtimeService.createMessageCorrelation(EventToMessage.MESSAGE_SUPPRESSED)
          .setVariables(processVariables);

    } else if (event.getSource() instanceof V4SharedPost) {
      messageCorrelation = runtimeService.createMessageCorrelation(EventToMessage.POST_SHARED)
          .setVariables(processVariables);

    } else if (event.getSource() instanceof V4InstantMessageCreated) {
      messageCorrelation = runtimeService.createMessageCorrelation(EventToMessage.IM_CREATED)
          .setVariables(processVariables);

    } else if (event.getSource() instanceof V4UserRequestedToJoinRoom) {
      messageCorrelation = runtimeService.createMessageCorrelation(EventToMessage.USER_REQUESTED_JOIN_ROOM)
          .setVariables(processVariables);

    } else if (event.getSource() instanceof V4ConnectionRequested) {
      messageCorrelation = runtimeService.createMessageCorrelation(EventToMessage.CONNECTION_REQUESTED)
          .setVariables(processVariables);

    } else if (event.getSource() instanceof V4ConnectionAccepted) {
      messageCorrelation = runtimeService.createMessageCorrelation(EventToMessage.CONNECTION_ACCEPTED)
          .setVariables(processVariables);
    }
    return messageCorrelation;
  }
}
