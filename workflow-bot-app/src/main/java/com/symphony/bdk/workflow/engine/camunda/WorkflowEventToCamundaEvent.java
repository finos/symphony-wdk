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
import com.symphony.bdk.workflow.swadl.v1.event.RequestReceivedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.TimerFiredEvent;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  private static final String MESSAGE_PREFIX = "message-received_";
  private static final String MESSAGE_SUPPRESSED = "message-suppressed";
  private static final String POST_SHARED = "post-shared";
  private static final String IM_CREATED = "im-created";
  public static final String FORM_REPLY_PREFIX = "form-reply_";
  private static final String ROOM_CREATED = "room-created";
  private static final String ROOM_UPDATED = "room-updated";
  private static final String ROOM_DEACTIVATED = "room-deactivated";
  private static final String ROOM_REACTIVATED = "room-reactivated";
  private static final String ROOM_MEMBER_PROMOTED_TO_OWNER = "room-member-promoted-to-owner-event";
  private static final String ROOM_MEMBER_DEMOTED_FROM_OWNER = "room-member-demoted-from-owner-event";
  private static final String USER_REQUESTED_JOIN_ROOM = "user-requested-join-room";
  private static final String USER_JOINED_ROOM = "user-joined-room";
  private static final String USER_LEFT_ROOM = "user-left-room";
  private static final String CONNECTION_REQUESTED = "connection-requested";
  private static final String CONNECTION_ACCEPTED = "connection-accepted";
  private static final String REQUEST_RECEIVED = "request-received";
  private static final String TIMER_FIRED_DATE = "timerFired_date";
  private static final String TIMER_FIRED_CYCLE = "timerFired_cycle";


  private static final AntPathMatcher MESSAGE_RECEIVED_CONTENT_MATCHER = new AntPathMatcher();

  private final RuntimeService runtimeService;

  private final SessionService sessionService;

  public String toTimerFiredEventName(TimerFiredEvent event) {
    if (StringUtils.isNotEmpty(event.getRepeat())) {
      return TIMER_FIRED_CYCLE;
    }
    return TIMER_FIRED_DATE;
  }

  public Optional<String> toSignalName(Event event, Workflow workflow) {
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

    } else if (event.getFormReplied() != null) {
      return Optional.of(String.format("%s%s", FORM_REPLY_PREFIX, event.getFormReplied().getFormId()));

    } else if (event.getOneOf() != null && !event.getOneOf().isEmpty()) {
      return toSignalName(event.getOneOf().get(0), workflow);

    } else if (event.getRequestReceived() != null) {
      return Optional.of(requestReceivedWorkflowEvent(workflow.getId()));
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
    Map<String, Object> args = eventSource.getArguments();
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
