package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.core.service.message.exception.PresentationMLParserException;
import com.symphony.bdk.core.service.message.util.PresentationMLParser;
import com.symphony.bdk.core.service.session.SessionService;
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
import com.symphony.bdk.workflow.swadl.v1.Event;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.camunda.bpm.engine.runtime.SignalEventReceivedBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// There might be a way to make this more generic/less code but waiting for
// event filtering to see how it is going to evolve, at least it is easy to understand.
@Component
public class WorkflowEventToCamundaEvent {

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

  @Autowired
  private SessionService sessionService;

  public Optional<String> toSignalName(Event event) {
    if (event.getMessageReceived() != null) {
      if (event.getMessageReceived().isRequiresBotMention()) {
        // this is super fragile, extra spaces, bot changing names are not handled
        // SlashCommand in the BDK does it this way though
        String displayName = sessionService.getSession().getDisplayName();
        return Optional.of(
            String.format("%s@%s %s", MESSAGE_PREFIX, displayName, event.getMessageReceived().getContent()));
      } else {
        return Optional.of(MESSAGE_PREFIX + event.getMessageReceived().getContent());
      }

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

    } else if (event.getRoomReactivated() != null) {
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

    } else if (event.getOneOf() != null && !event.getOneOf().isEmpty()) {
      return toSignalName(event.getOneOf().get(0));
    }

    return Optional.empty();
  }

  public <T> List<MessageCorrelationBuilder> dispatch(RealTimeEvent<T> event)
      throws PresentationMLParserException {
    SignalEventReceivedBuilder messageCorrelation = null;

    Map<String, Object> processVariables = new HashMap<>();
    processVariables.put(ActivityExecutorContext.EVENT, new EventHolder<>(event.getInitiator(), event.getSource()));

    if (event.getSource() instanceof V4SymphonyElementsAction) {
      formReplyToMessage(event, processVariables);

    } else if (event.getSource() instanceof V4MessageSent) {
      return messageSentToMessage((RealTimeEvent<V4MessageSent>) event, processVariables);

    } else if (event.getSource() instanceof V4RoomCreated) {
      runtimeService.createSignalEvent(WorkflowEventToCamundaEvent.ROOM_CREATED)
          .setVariables(processVariables).send();

    } else if (event.getSource() instanceof V4RoomUpdated) {
      runtimeService.createSignalEvent(WorkflowEventToCamundaEvent.ROOM_UPDATED)
          .setVariables(processVariables).send();

    } else if (event.getSource() instanceof V4RoomDeactivated) {
      runtimeService.createSignalEvent(WorkflowEventToCamundaEvent.ROOM_DEACTIVATED)
          .setVariables(processVariables).send();

    } else if (event.getSource() instanceof V4RoomReactivated) {
      runtimeService.createSignalEvent(WorkflowEventToCamundaEvent.ROOM_REACTIVATED)
          .setVariables(processVariables).send();

    } else if (event.getSource() instanceof V4UserJoinedRoom) {
      runtimeService.createSignalEvent(WorkflowEventToCamundaEvent.USER_JOINED_ROOM)
          .setVariables(processVariables).send();

    } else if (event.getSource() instanceof V4UserLeftRoom) {
      runtimeService.createSignalEvent(WorkflowEventToCamundaEvent.USER_LEFT_ROOM)
          .setVariables(processVariables).send();

    } else if (event.getSource() instanceof V4RoomMemberDemotedFromOwner) {
      runtimeService.createSignalEvent(WorkflowEventToCamundaEvent.ROOM_MEMBER_DEMOTED_FROM_OWNER)
          .setVariables(processVariables).send();

    } else if (event.getSource() instanceof V4RoomMemberPromotedToOwner) {
      runtimeService.createSignalEvent(WorkflowEventToCamundaEvent.ROOM_MEMBER_PROMOTED_TO_OWNER)
          .setVariables(processVariables).send();

    } else if (event.getSource() instanceof V4MessageSuppressed) {
      runtimeService.createSignalEvent(WorkflowEventToCamundaEvent.MESSAGE_SUPPRESSED)
          .setVariables(processVariables).send();

    } else if (event.getSource() instanceof V4SharedPost) {
      runtimeService.createSignalEvent(WorkflowEventToCamundaEvent.POST_SHARED)
          .setVariables(processVariables).send();

    } else if (event.getSource() instanceof V4InstantMessageCreated) {
      runtimeService.createSignalEvent(WorkflowEventToCamundaEvent.IM_CREATED)
          .setVariables(processVariables).send();

    } else if (event.getSource() instanceof V4UserRequestedToJoinRoom) {
      runtimeService.createSignalEvent(WorkflowEventToCamundaEvent.USER_REQUESTED_JOIN_ROOM)
          .setVariables(processVariables).send();

    } else if (event.getSource() instanceof V4ConnectionRequested) {
      runtimeService.createSignalEvent(WorkflowEventToCamundaEvent.CONNECTION_REQUESTED)
          .setVariables(processVariables).send();

    } else if (event.getSource() instanceof V4ConnectionAccepted) {
      runtimeService.createSignalEvent(WorkflowEventToCamundaEvent.CONNECTION_ACCEPTED)
          .setVariables(processVariables).send();
    }
    return Collections.emptyList();
  }

  private <T> MessageCorrelationBuilder formReplyToMessage(RealTimeEvent<T> event,
      Map<String, Object> processVariables) {
    MessageCorrelationBuilder messageCorrelation;
    // we expect the activity id to be the same as the form id to work
    // correlation across processes is based on the message id tha was created to send the form
    V4SymphonyElementsAction implEvent = (V4SymphonyElementsAction) event.getSource();
    Map<String, Object> formReplies = (Map<String, Object>) implEvent.getFormValues();
    String formId = implEvent.getFormId();
    processVariables.put(formId, formReplies);
    runtimeService.createMessageCorrelation(WorkflowEventToCamundaEvent.FORM_REPLY_PREFIX + formId)
        .processInstanceVariableEquals(formId + ".outputs.msgId", implEvent.getFormMessageId())
        .setVariables(processVariables)
        .correlateAll();
    return null;
  }

  private List<MessageCorrelationBuilder> messageSentToMessage(RealTimeEvent<V4MessageSent> event,
      Map<String, Object> processVariables) throws PresentationMLParserException {
    String presentationMl = event.getSource().getMessage().getMessage();
    String textContent = PresentationMLParser.getTextContent(presentationMl);

    List<MessageCorrelationBuilder> messages = new ArrayList<>();
    runtimeService.createSignalEvent(WorkflowEventToCamundaEvent.MESSAGE_PREFIX + textContent)
        .setVariables(processVariables).send();
    // we send 2 messages to correlate if a content is set or not
    // this will change with the generalization of event filtering
    runtimeService.createSignalEvent(WorkflowEventToCamundaEvent.MESSAGE_PREFIX)
        .setVariables(processVariables).send();
    return messages;
  }
}
