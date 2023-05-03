package com.symphony.bdk.workflow.event;

import com.symphony.bdk.workflow.swadl.v1.Event;
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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.util.CollectionUtils;

import java.util.Optional;

@RequiredArgsConstructor
public enum WorkflowEventType implements EventVisitor {
  MESSAGE_RECEIVED("message-received_") {
    @Override
    public boolean predict(Event event) {
      return Optional.ofNullable(event.getMessageReceived()).isPresent();
    }

    @Override
    public Triple<String, String, Class<?>> getEventTripleInfo(Event event, String workflowId, String botName) {
      if (event.getMessageReceived().isRequiresBotMention()) {
        // this is super fragile, extra spaces, bot changing names are not handled
        // SlashCommand in the BDK does it this way though
        return Triple.of(event.getMessageReceived().getId(),
            String.format("%s@%s %s", this.getEventName(), botName, event.getMessageReceived().getContent()),
            MessageReceivedEvent.class);
      } else {
        return Triple.of(event.getMessageReceived().getId(),
            this.getEventName() + event.getMessageReceived().getContent(),
            MessageReceivedEvent.class);
      }
    }
  },
  MESSAGE_SUPPRESSED("message-suppressed") {
    @Override
    public boolean predict(Event event) {
      return Optional.ofNullable(event.getMessageSuppressed()).isPresent();
    }

    @Override
    public Triple<String, String, Class<?>> getEventTripleInfo(Event event, String workflowId, String botName) {
      return Triple.of(event.getMessageSuppressed().getId(), this.getEventName(), MessageSuppressedEvent.class);
    }
  },
  IM_CREATED("im-created") {
    @Override
    public boolean predict(Event event) {
      return Optional.ofNullable(event.getImCreated()).isPresent();
    }

    @Override
    public Triple<String, String, Class<?>> getEventTripleInfo(Event event, String workflowId, String botName) {
      return Triple.of(event.getImCreated().getId(), this.getEventName(), ImCreatedEvent.class);
    }
  },
  POST_SHARED("post-shared") {
    @Override
    public boolean predict(Event event) {
      return Optional.ofNullable(event.getPostShared()).isPresent();
    }

    @Override
    public Triple<String, String, Class<?>> getEventTripleInfo(Event event, String workflowId, String botName) {
      return Triple.of(event.getPostShared().getId(), this.getEventName(), PostSharedEvent.class);
    }
  },
  ROOM_CREATED("room-created") {
    @Override
    public boolean predict(Event event) {
      return Optional.ofNullable(event.getRoomCreated()).isPresent();
    }

    @Override
    public Triple<String, String, Class<?>> getEventTripleInfo(Event event, String workflowId, String botName) {
      return Triple.of(event.getRoomCreated().getId(), this.getEventName(), RoomCreatedEvent.class);
    }
  },
  ROOM_UPDATED("room-updated") {
    @Override
    public boolean predict(Event event) {
      return Optional.ofNullable(event.getRoomUpdated()).isPresent();
    }

    @Override
    public Triple<String, String, Class<?>> getEventTripleInfo(Event event, String workflowId, String botName) {
      return Triple.of(event.getRoomUpdated().getId(), this.getEventName(), RoomUpdatedEvent.class);
    }
  },
  ROOM_DEACTIVATED("room-deactivated") {
    @Override
    public boolean predict(Event event) {
      return Optional.ofNullable(event.getRoomDeactivated()).isPresent();
    }

    @Override
    public Triple<String, String, Class<?>> getEventTripleInfo(Event event, String workflowId, String botName) {
      return Triple.of(event.getRoomDeactivated().getId(), this.getEventName(), RoomDeactivatedEvent.class);
    }
  },
  ROOM_REACTIVATED("room-reactivated") {
    @Override
    public boolean predict(Event event) {
      return Optional.ofNullable(event.getRoomReactivated()).isPresent();
    }

    @Override
    public Triple<String, String, Class<?>> getEventTripleInfo(Event event, String workflowId, String botName) {
      return Triple.of(event.getRoomReactivated().getId(), this.getEventName(), RoomReactivatedEvent.class);
    }
  },
  ROOM_MEMBER_PROMOTED_TO_OWNER("room-member-promoted-to-owner-event") {
    @Override
    public boolean predict(Event event) {
      return Optional.ofNullable(event.getRoomMemberPromotedToOwner()).isPresent();
    }

    @Override
    public Triple<String, String, Class<?>> getEventTripleInfo(Event event, String workflowId, String botName) {
      return Triple.of(event.getRoomMemberPromotedToOwner().getId(), this.getEventName(),
          RoomMemberPromotedToOwnerEvent.class);
    }
  },
  ROOM_MEMBER_DEMOTED_FROM_OWNER("room-member-demoted-from-owner-event") {
    @Override
    public boolean predict(Event event) {
      return Optional.ofNullable(event.getRoomMemberDemotedFromOwner()).isPresent();
    }

    @Override
    public Triple<String, String, Class<?>> getEventTripleInfo(Event event, String workflowId, String botName) {
      return Triple.of(event.getRoomMemberDemotedFromOwner().getId(), this.getEventName(),
          RoomMemberDemotedFromOwnerEvent.class);
    }
  },
  USER_REQUESTED_JOIN_ROOM("user-requested-join-room") {
    @Override
    public boolean predict(Event event) {
      return Optional.ofNullable(event.getUserRequestedJoinRoom()).isPresent();
    }

    @Override
    public Triple<String, String, Class<?>> getEventTripleInfo(Event event, String workflowId, String botName) {
      return Triple.of(event.getUserRequestedJoinRoom().getId(), this.getEventName(),
          UserRequestedToJoinRoomEvent.class);
    }
  },
  USER_JOINED_ROOM("user-joined-room") {
    @Override
    public boolean predict(Event event) {
      return Optional.ofNullable(event.getUserJoinedRoom()).isPresent();
    }

    @Override
    public Triple<String, String, Class<?>> getEventTripleInfo(Event event, String workflowId, String botName) {
      return Triple.of(event.getUserJoinedRoom().getId(), this.getEventName(), UserJoinedRoomEvent.class);
    }
  },
  USER_LEFT_ROOM("user-left-room") {
    @Override
    public boolean predict(Event event) {
      return Optional.ofNullable(event.getUserLeftRoom()).isPresent();
    }

    @Override
    public Triple<String, String, Class<?>> getEventTripleInfo(Event event, String workflowId, String botName) {
      return Triple.of(event.getUserLeftRoom().getId(), this.getEventName(), UserLeftRoomEvent.class);
    }
  },
  CONNECTION_REQUESTED("connection-requested") {
    @Override
    public boolean predict(Event event) {
      return Optional.ofNullable(event.getConnectionRequested()).isPresent();
    }

    @Override
    public Triple<String, String, Class<?>> getEventTripleInfo(Event event, String workflowId, String botName) {
      return Triple.of(event.getConnectionRequested().getId(), this.getEventName(), ConnectionRequestedEvent.class);
    }
  },
  CONNECTION_ACCEPTED("connection-accepted") {
    @Override
    public boolean predict(Event event) {
      return Optional.ofNullable(event.getConnectionAccepted()).isPresent();
    }

    @Override
    public Triple<String, String, Class<?>> getEventTripleInfo(Event event, String workflowId, String botName) {
      return Triple.of(event.getConnectionAccepted().getId(), this.getEventName(), ConnectionAcceptedEvent.class);
    }
  },
  FORM_REPLIED("form-reply_") {
    @Override
    public boolean predict(Event event) {
      return Optional.ofNullable(event.getFormReplied()).isPresent();
    }

    @Override
    public Triple<String, String, Class<?>> getEventTripleInfo(Event event, String workflowId, String botName) {
      return Triple.of(event.getFormReplied().getId(),
          String.format("%s%s", this.getEventName(), event.getFormReplied().getFormId()), FormRepliedEvent.class);
    }
  },
  REQUEST_RECEIVED("request-received_") {
    @Override
    public boolean predict(Event event) {
      return Optional.ofNullable(event.getRequestReceived()).isPresent();
    }

    @Override
    public Triple<String, String, Class<?>> getEventTripleInfo(Event event, String workflowId, String botName) {
      return Triple.of(event.getRequestReceived().getId(), this.getEventName() + workflowId,
          RequestReceivedEvent.class);
    }
  },
  TIME_FIRED("timerFired_date") {
    @Override
    public boolean predict(Event event) {
      return Optional.ofNullable(event.getTimerFired()).isPresent();
    }

    @Override
    public Triple<String, String, Class<?>> getEventTripleInfo(Event event, String workflowId, String botName) {
      String name = this.getEventName();
      if (StringUtils.isNotEmpty(event.getTimerFired().getRepeat())) {
        name = "timerFired_cycle_" + event.getTimerFired().getRepeat();
      } else {
        name += "_" + event.getTimerFired().getAt();
      }
      return Triple.of(event.getTimerFired().getId(), name, TimerFiredEvent.class);
    }
  },
  ONE_OF("") {
    @Override
    public boolean predict(Event event) {
      return !CollectionUtils.isEmpty(event.getOneOf());
    }

    @Override
    public Triple<String, String, Class<?>> getEventTripleInfo(Event event, String workflowId, String botName) {
      return WorkflowEventType.getEventType(event.getOneOf().get(0))
          .get()
          .getEventTripleInfo(event.getOneOf().get(0), workflowId, botName);
    }
  };

  @Getter
  private final String eventName;

  public static Optional<WorkflowEventType> getEventType(Event event) {
    for (WorkflowEventType type : WorkflowEventType.values()) {
      if (type.predict(event)) {
        return Optional.of(type);
      }
    }
    return Optional.empty();
  }
}
