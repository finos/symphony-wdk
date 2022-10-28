package com.symphony.bdk.workflow.engine.camunda;

import static java.util.Collections.singletonMap;

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
import com.symphony.bdk.workflow.engine.camunda.variable.FormVariableListener;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.EventHolder;
import com.symphony.bdk.workflow.engine.executor.message.SendMessageExecutor;
import com.symphony.bdk.workflow.swadl.v1.Event;
import com.symphony.bdk.workflow.swadl.v1.Workflow;
import com.symphony.bdk.workflow.swadl.v1.event.ConnectionAcceptedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.ConnectionRequestedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.FormRepliedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.ImCreatedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.MessageReceivedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.MessageSuppressedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.PostSharedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RequestReceivedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RoomCreatedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RoomDeactivatedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RoomMemberDemotedFromOwnerEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RoomMemberPromotedToOwnerEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RoomReactivatedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.RoomUpdatedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.TimerFiredEvent;
import com.symphony.bdk.workflow.swadl.v1.event.UserJoinedRoomEvent;
import com.symphony.bdk.workflow.swadl.v1.event.UserLeftRoomEvent;
import com.symphony.bdk.workflow.swadl.v1.event.UserRequestedToJoinRoomEvent;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.impl.event.EventType;
import org.camunda.bpm.engine.runtime.EventSubscription;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// There might be a way to make this more generic/less code but waiting for
// event filtering to see how it is going to evolve, at least it is easy to understand.
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEventToCamundaEvent {


  public static final String EVENT_NAME = "eventName";
  public static final String MESSAGE_PREFIX = "message-received_";
  public static final String MESSAGE_SUPPRESSED = "message-suppressed";
  public static final String POST_SHARED = "post-shared";
  private static final String IM_CREATED = "im-created";
  public static final String FORM_REPLY_PREFIX = "form-reply_";
  public static final String ROOM_CREATED = "room-created";
  public static final String ROOM_UPDATED = "room-updated";
  public static final String ROOM_DEACTIVATED = "room-deactivated";
  public static final String ROOM_REACTIVATED = "room-reactivated";
  public static final String ROOM_MEMBER_PROMOTED_TO_OWNER = "room-member-promoted-to-owner-event";
  public static final String ROOM_MEMBER_DEMOTED_FROM_OWNER = "room-member-demoted-from-owner-event";
  public static final String USER_REQUESTED_JOIN_ROOM = "user-requested-join-room";
  public static final String USER_JOINED_ROOM = "user-joined-room";
  public static final String USER_LEFT_ROOM = "user-left-room";
  public static final String CONNECTION_REQUESTED = "connection-requested";
  public static final String CONNECTION_ACCEPTED = "connection-accepted";
  public static final String REQUEST_RECEIVED = "request-received";
  public static final String TIMER_FIRED_DATE = "timerFired_date";
  public static final String TIMER_FIRED_CYCLE = "timerFired_cycle";


  private static final AntPathMatcher MESSAGE_RECEIVED_CONTENT_MATCHER = new AntPathMatcher();

  private final RuntimeService runtimeService;

  private final SessionService sessionService;

  public String toTimerFiredEventName(TimerFiredEvent event) {
    if (StringUtils.isNotEmpty(event.getRepeat())) {
      return TIMER_FIRED_CYCLE;
    }
    return TIMER_FIRED_DATE;
  }

  /**
   * Find the event identifers
   *
   * @param event    the wrapper event
   * @param workflow swadl workflow model
   * @return triple contains from left to right = eventId, id, the type of the event or activity
   */
  public Optional<Triple<String, String, Class<?>>> toSignalName(Event event, Workflow workflow) {
    if (event.getMessageReceived() != null) {
      if (event.getMessageReceived().isRequiresBotMention()) {
        // this is super fragile, extra spaces, bot changing names are not handled
        // SlashCommand in the BDK does it this way though
        String displayName = sessionService.getSession().getDisplayName();
        return Optional.of(Triple.of(event.getMessageReceived().getId(),
            String.format("%s@%s %s", MESSAGE_PREFIX, displayName, event.getMessageReceived().getContent()),
            MessageReceivedEvent.class));
      } else {
        return Optional.of(
            Triple.of(event.getMessageReceived().getId(), MESSAGE_PREFIX + event.getMessageReceived().getContent(),
                MessageReceivedEvent.class));
      }

    } else if (event.getMessageSuppressed() != null) {
      return Optional.of(
          Triple.of(event.getMessageSuppressed().getId(), MESSAGE_SUPPRESSED, MessageSuppressedEvent.class));

    } else if (event.getImCreated() != null) {
      return Optional.of(Triple.of(event.getImCreated().getId(), IM_CREATED, ImCreatedEvent.class));

    } else if (event.getPostShared() != null) {
      return Optional.of(Triple.of(event.getPostShared().getId(), POST_SHARED, PostSharedEvent.class));

    } else if (event.getRoomCreated() != null) {
      return Optional.of(Triple.of(event.getRoomCreated().getId(), ROOM_CREATED, RoomCreatedEvent.class));

    } else if (event.getRoomUpdated() != null) {
      return Optional.of(Triple.of(event.getRoomUpdated().getId(), ROOM_UPDATED, RoomUpdatedEvent.class));

    } else if (event.getRoomDeactivated() != null) {
      return Optional.of(Triple.of(event.getRoomDeactivated().getId(), ROOM_DEACTIVATED, RoomDeactivatedEvent.class));

    } else if (event.getRoomReactivated() != null) {
      return Optional.of(Triple.of(event.getRoomReactivated().getId(), ROOM_REACTIVATED, RoomReactivatedEvent.class));

    } else if (event.getRoomMemberPromotedToOwner() != null) {
      return Optional.of(Triple.of(event.getRoomMemberPromotedToOwner().getId(), ROOM_MEMBER_PROMOTED_TO_OWNER,
          RoomMemberPromotedToOwnerEvent.class));

    } else if (event.getRoomMemberDemotedFromOwner() != null) {
      return Optional.of(Triple.of(event.getRoomMemberDemotedFromOwner().getId(), ROOM_MEMBER_DEMOTED_FROM_OWNER,
          RoomMemberDemotedFromOwnerEvent.class));

    } else if (event.getUserRequestedJoinRoom() != null) {
      return Optional.of(Triple.of(event.getUserRequestedJoinRoom().getId(), USER_REQUESTED_JOIN_ROOM,
          UserRequestedToJoinRoomEvent.class));

    } else if (event.getUserJoinedRoom() != null) {
      return Optional.of(Triple.of(event.getUserJoinedRoom().getId(), USER_JOINED_ROOM, UserJoinedRoomEvent.class));

    } else if (event.getUserLeftRoom() != null) {
      return Optional.of(Triple.of(event.getUserLeftRoom().getId(), USER_LEFT_ROOM, UserLeftRoomEvent.class));

    } else if (event.getConnectionRequested() != null) {
      return Optional.of(
          Triple.of(event.getConnectionRequested().getId(), CONNECTION_REQUESTED, ConnectionRequestedEvent.class));

    } else if (event.getConnectionAccepted() != null) {
      return Optional.of(
          Triple.of(event.getConnectionAccepted().getId(), CONNECTION_ACCEPTED, ConnectionAcceptedEvent.class));

    } else if (event.getFormReplied() != null) {
      return Optional.of(Triple.of(event.getFormReplied().getId(),
          String.format("%s%s", FORM_REPLY_PREFIX, event.getFormReplied().getFormId()), FormRepliedEvent.class));

    } else if (event.getOneOf() != null && !event.getOneOf().isEmpty()) {
      return toSignalName(event.getOneOf().get(0), workflow);

    } else if (event.getRequestReceived() != null) {
      return Optional.of(Triple.of(event.getRequestReceived().getId(), requestReceivedWorkflowEvent(workflow.getId()),
          RequestReceivedEvent.class));
    }

    return Optional.empty();
  }

  @SuppressWarnings("unchecked")
  public <T> void dispatch(RealTimeEvent<T> event) throws PresentationMLParserException {
    Map<String, Object> processVariables = new HashMap<>();
    processVariables.put(ActivityExecutorContext.EVENT,
        new EventHolder<>(event.getInitiator(), event.getSource(), new HashMap<>()));

    if (event.getInitiator() != null
        && event.getInitiator().getUser() != null
        && event.getInitiator().getUser().getUserId() != null) {
      Long userId = event.getInitiator().getUser().getUserId();
      processVariables.put(ActivityExecutorContext.INITIATOR, userId);

      log.debug("Dispatching event {} from user {}", event.getSource().getClass().getSimpleName(), userId);
    }

    if (event.getSource() instanceof V4SymphonyElementsAction) {
      formReplyToMessage(event, processVariables);

    } else if (event.getSource() instanceof V4MessageSent) {
      messageSentToMessage((RealTimeEvent<V4MessageSent>) event, processVariables);

    } else if (event.getSource() instanceof V4RoomCreated) {
      log.debug("received a room created event");
      this.createSignalEvent(processVariables, ROOM_CREATED);

    } else if (event.getSource() instanceof V4RoomUpdated) {
      log.debug("received a room updated event");
      this.createSignalEvent(processVariables, ROOM_UPDATED);

    } else if (event.getSource() instanceof V4RoomDeactivated) {
      log.debug("received a room reactivated event");
      this.createSignalEvent(processVariables, ROOM_DEACTIVATED);

    } else if (event.getSource() instanceof V4RoomReactivated) {
      log.debug("received a room reactivated event");
      this.createSignalEvent(processVariables, ROOM_REACTIVATED);

    } else if (event.getSource() instanceof V4UserJoinedRoom) {
      log.debug("received an user joined room event");
      this.createSignalEvent(processVariables, USER_JOINED_ROOM);

    } else if (event.getSource() instanceof V4UserLeftRoom) {
      log.debug("received an user left room event");
      this.createSignalEvent(processVariables, USER_LEFT_ROOM);

    } else if (event.getSource() instanceof V4RoomMemberDemotedFromOwner) {
      log.debug("received a member demoted from owner event");
      this.createSignalEvent(processVariables, ROOM_MEMBER_DEMOTED_FROM_OWNER);

    } else if (event.getSource() instanceof V4RoomMemberPromotedToOwner) {
      log.debug("received a room member promoted to owner event");
      this.createSignalEvent(processVariables, ROOM_MEMBER_PROMOTED_TO_OWNER);

    } else if (event.getSource() instanceof V4MessageSuppressed) {
      log.debug("received a message suppressed event");
      this.createSignalEvent(processVariables, MESSAGE_SUPPRESSED);

    } else if (event.getSource() instanceof V4SharedPost) {
      log.debug("received a message suppressed event");
      this.createSignalEvent(processVariables, POST_SHARED);

    } else if (event.getSource() instanceof V4InstantMessageCreated) {
      log.debug("received a IM created event");
      this.createSignalEvent(processVariables, IM_CREATED);

    } else if (event.getSource() instanceof V4UserRequestedToJoinRoom) {
      log.debug("received a user join room request event");
      this.createSignalEvent(processVariables, USER_REQUESTED_JOIN_ROOM);

    } else if (event.getSource() instanceof V4ConnectionRequested) {
      log.debug("received a connection requested event");
      this.createSignalEvent(processVariables, CONNECTION_REQUESTED);

    } else if (event.getSource() instanceof V4ConnectionAccepted) {
      log.debug("received a connection accepted event");
      this.createSignalEvent(processVariables, CONNECTION_ACCEPTED);

    } else if (event.getSource() instanceof RequestReceivedEvent) {
      log.debug("received a request received event");
      requestReceivedToRequest((RequestReceivedEvent) event.getSource(), processVariables);
    }
  }

  private void createSignalEvent(Map<String, Object> variables, String eventName) {
    ((EventHolder) variables.get(ActivityExecutorContext.EVENT)).getArgs().put(EVENT_NAME, eventName);
    runtimeService.createSignalEvent(eventName).setVariables(variables).send();
  }

  @SuppressWarnings("unchecked")
  private <T> void formReplyToMessage(RealTimeEvent<T> event, Map<String, Object> processVariables) {
    // we expect the activity id to be the same as the form id to work
    // correlation across processes is based on the message id that was created to send the form
    V4SymphonyElementsAction implEvent = (V4SymphonyElementsAction) event.getSource();
    log.debug("received form reply [{}]", implEvent.getFormId());
    Map<String, Object> formReplies = (Map<String, Object>) implEvent.getFormValues();
    String formId = implEvent.getFormId();
    processVariables.put(FormVariableListener.FORM_VARIABLES, singletonMap(formId, formReplies));

    ((EventHolder) processVariables.get(ActivityExecutorContext.EVENT)).getArgs()
        .put(EVENT_NAME, FORM_REPLY_PREFIX + formId);

    runtimeService.createMessageCorrelation(WorkflowEventToCamundaEvent.FORM_REPLY_PREFIX + formId)
        .processInstanceVariableEquals(
            String.format("%s.%s.%s", formId, ActivityExecutorContext.OUTPUTS,
                SendMessageExecutor.OUTPUT_MESSAGE_ID_KEY), implEvent.getFormMessageId())
        .setVariables(processVariables)
        .correlateAll();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void requestReceivedToRequest(RequestReceivedEvent eventSource, Map<String, Object> processVariables) {
    String eventName = requestReceivedWorkflowEvent(eventSource.getWorkflowId());
    Map<String, Object> args;

    if (eventSource.getArguments() == null) {
      args = new HashMap<>();
    } else {
      args = new HashMap<>(eventSource.getArguments());
    }

    args.put(EVENT_NAME, eventName);
    ((EventHolder) processVariables.get(ActivityExecutorContext.EVENT)).setArgs(args);

    runtimeService.createSignalEvent(eventName).setVariables(processVariables).send();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void messageSentToMessage(RealTimeEvent<V4MessageSent> event,
      Map<String, Object> processVariables) throws PresentationMLParserException {

    // Event's message cannot be null, this if statement is only added to fix Sonar warnings
    if (event.getSource().getMessage() != null) {
      log.debug("received message [{}]", event.getSource().getMessage().getMessageId());
      log.trace("received message [{}]", event.getSource().getMessage().getMessage());
      String presentationMl = event.getSource().getMessage().getMessage();
      String receivedContent = PresentationMLParser.getTextContent(presentationMl);

      runtimeService.createMessageCorrelation(MESSAGE_PREFIX + receivedContent)
          .setVariables(processVariables)
          .correlateAll();

      List<EventSubscription> subscribedSignals = runtimeService.createEventSubscriptionQuery()
          .eventType(EventType.SIGNAL.name())
          .list();

      // we want to avoid sending the same signals twice otherwise workflows would be triggered multiple times
      // meaning only the first matching /command is picked
      Set<String> alreadySentSignals = new HashSet<>();
      for (EventSubscription signal : subscribedSignals) {
        if (!alreadySentSignals.contains(signal.getEventName())) {
          String content = messageReceivedContentFromSignalName(signal.getEventName());
          if (MESSAGE_RECEIVED_CONTENT_MATCHER.match(content, receivedContent)) {
            // match the arguments and add them to the event holder
            Map<String, String> args =
                MESSAGE_RECEIVED_CONTENT_MATCHER.extractUriTemplateVariables(content, receivedContent);
            args.put(EVENT_NAME, signal.getEventName());
            ((EventHolder) processVariables.get(ActivityExecutorContext.EVENT)).setArgs(args);

            runtimeService.createSignalEvent(signal.getEventName())
                .setVariables(processVariables)
                .send();
            alreadySentSignals.add(signal.getEventName());
          }
        }
      }

      // we send another signal for workflows listening to any message (without content being set)
      runtimeService.createSignalEvent(WorkflowEventToCamundaEvent.MESSAGE_PREFIX)
          .setVariables(processVariables)
          .send();
    }
  }

  private static String messageReceivedContentFromSignalName(String signalName) {
    return signalName.replace(MESSAGE_PREFIX, "");
  }

  // request received event triggers a specific workflow/deployment only, not all the workflows listening for that event
  private static String requestReceivedWorkflowEvent(String workflowId) {
    return REQUEST_RECEIVED + "_" + workflowId;
  }
}
