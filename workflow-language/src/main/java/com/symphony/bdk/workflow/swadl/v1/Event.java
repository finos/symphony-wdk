package com.symphony.bdk.workflow.swadl.v1;

import com.symphony.bdk.workflow.swadl.v1.event.ActivityCompletedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.ActivityExpiredEvent;
import com.symphony.bdk.workflow.swadl.v1.event.ActivityFailedEvent;
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {
  @JsonProperty
  private List<Event> oneOf;

  @JsonProperty
  private List<Event> allOf;

  @JsonProperty
  private FormRepliedEvent formReplied;

  @JsonProperty
  private ActivityExpiredEvent activityExpired;

  @JsonProperty
  private ActivityCompletedEvent activityCompleted;

  @JsonProperty
  private ActivityFailedEvent activityFailed;

  @JsonProperty
  private MessageReceivedEvent messageReceived;

  @JsonProperty
  private MessageSuppressedEvent messageSuppressed;

  @JsonProperty
  private PostSharedEvent postShared;

  @JsonProperty
  private ImCreatedEvent imCreated;

  @JsonProperty
  private RoomCreatedEvent roomCreated;

  @JsonProperty
  private RoomUpdatedEvent roomUpdated;

  @JsonProperty
  private RoomDeactivatedEvent roomDeactivated;

  @JsonProperty
  private RoomReactivatedEvent roomReactivated;

  @JsonProperty
  private RoomMemberPromotedToOwnerEvent roomMemberPromotedToOwner;

  @JsonProperty
  private RoomMemberPromotedToOwnerEvent roomMemberDemotedFromOwner;

  @JsonProperty
  private UserJoinedRoomEvent userJoinedRoom;

  @JsonProperty
  private UserLeftRoomEvent userLeftRoom;

  @JsonProperty
  private UserRequestedToJoinRoomEvent userRequestedJoinRoom;

  @JsonProperty
  private ConnectionRequestedEvent connectionRequested;

  @JsonProperty
  private ConnectionAcceptedEvent connectionAccepted;

  @JsonProperty
  private TimerFiredEvent timerFired;

  @JsonProperty
  private RequestReceivedEvent requestReceived;

  public String getEventType() {

    if (this.formReplied != null) {
      return FormRepliedEvent.class.getSimpleName();
    }

    if (this.activityExpired != null) {
      return ActivityExpiredEvent.class.getSimpleName();
    }

    if (this.activityCompleted != null) {
      return ActivityCompletedEvent.class.getSimpleName();
    }

    if (this.activityFailed != null) {
      return ActivityFailedEvent.class.getSimpleName();
    }

    if (this.messageReceived != null) {
      return MessageReceivedEvent.class.getSimpleName();
    }

    if (this.messageSuppressed != null) {
      return MessageSuppressedEvent.class.getSimpleName();
    }

    if (this.postShared != null) {
      return PostSharedEvent.class.getSimpleName();
    }

    if (this.imCreated != null) {
      return ImCreatedEvent.class.getSimpleName();
    }

    if (this.roomCreated != null) {
      return RoomCreatedEvent.class.getSimpleName();
    }

    if (this.roomUpdated != null) {
      return RoomUpdatedEvent.class.getSimpleName();
    }

    if (this.roomDeactivated != null) {
      return RoomDeactivatedEvent.class.getSimpleName();
    }

    if (this.roomReactivated != null) {
      return RoomReactivatedEvent.class.getSimpleName();
    }

    if (this.roomMemberPromotedToOwner != null) {
      return RoomMemberPromotedToOwnerEvent.class.getSimpleName();
    }

    if (this.roomMemberDemotedFromOwner != null) {
      return RoomMemberDemotedFromOwnerEvent.class.getSimpleName();
    }

    if (this.userJoinedRoom != null) {
      return UserJoinedRoomEvent.class.getSimpleName();
    }

    if (this.userLeftRoom != null) {
      return UserLeftRoomEvent.class.getSimpleName();
    }

    if (this.userRequestedJoinRoom != null) {
      return UserRequestedToJoinRoomEvent.class.getSimpleName();
    }

    if (this.connectionRequested != null) {
      return ConnectionRequestedEvent.class.getSimpleName();
    }

    if (this.connectionAccepted != null) {
      return ConnectionAcceptedEvent.class.getSimpleName();
    }

    if (this.timerFired != null) {
      return TimerFiredEvent.class.getSimpleName();
    }

    if (this.requestReceived != null) {
      return RequestReceivedEvent.class.getSimpleName();
    }

    return "";
  }

/*  public boolean isMessageReceived() {
    if (this.messageReceived != null) {
      return true;
    }

    if (this.getOneOf() != null) {
      boolean isMessageReceivedInOneOf =
          this.getOneOf().stream().anyMatch(event -> event.getMessageReceived() != null);
      if (isMessageReceivedInOneOf) {
        return true;
      }
    }

    // check in allOf
    return this.getAllOf() != null &&  this.getAllOf().stream().anyMatch(event -> event.getMessageReceived() != null);
  }

  public boolean isEventOfType(String type) {
    return getEventType().equals(type) || getAllOfEventTypes().contains(type) || getOneOfEventTypes().contains(type);
  }

  public List<String> getAllOfEventTypes() {
    if (this.allOf != null) {
      return this.allOf.stream().map(Event::getEventType).collect(Collectors.toList());
    }

    return Collections.emptyList();
  }

  public List<String> getOneOfEventTypes() {
    if (this.oneOf != null) {
      return this.oneOf.stream().map(Event::getEventType).collect(Collectors.toList());
    }

    return Collections.emptyList();
  }

  public String getMessageReceivedEventContent() {
    if (!this.isEventOfType(MessageReceivedEvent.class.getSimpleName())) {
      return null;
    }

    if (this.messageReceived != null) {
      return this.messageReceived.getContent();
    }

    if (this.oneOf != null) {
      Optional<Event> optional = this.oneOf
          .stream()
          .filter(event -> event.getMessageReceived() != null && !event.getMessageReceived().getContent().isBlank())
          .findFirst();

      if (optional.isPresent()) {
        return optional.get().getMessageReceived().getContent();
      }
    }

    if (this.allOf != null) {
      Optional<Event> optional = this.allOf
          .stream()
          .filter(event -> event.getMessageReceived() != null && !event.getMessageReceived().getContent().isBlank())
          .findFirst();

      if (optional.isPresent()) {
        return optional.get().getMessageReceived().getContent();
      }
    }

    return null;
  }

  /*public boolean isFormReplied() {
    if (this.formReplied != null) {
      return true;
    }

    if (this.getOneOf() != null) {
      boolean isFormRepliedInOneOf =
          this.getOneOf().stream().anyMatch(event -> event.getFormReplied() != null);
      if (isFormRepliedInOneOf) {
        return true;
      }
    }

    // check in allOf
    return this.getAllOf() != null && this.getAllOf().stream().anyMatch(event -> event.getFormReplied() != null);
  }

  public boolean isRoomCreated() {
    if (this.roomCreated != null) {
      return true;
    }

    if (this.getOneOf() != null) {
      boolean isRoomCreatedInOneOf = this.getOneOf().stream().anyMatch(event -> event.getRoomCreated() != null);
      if (isRoomCreatedInOneOf) {
        return true;
      }
    }

    // check in allOf
    return this.getAllOf() != null &&  this.getAllOf().stream().anyMatch(event -> event.getRoomCreated() != null);
  }

  public boolean isRoomUpdated() {
    if (this.roomUpdated != null) {
      return true;
    }

    if (this.getOneOf() != null) {
      boolean isRoomUpdatedInOneOf = this.getOneOf().stream().anyMatch(event -> event.getRoomUpdated() != null);
      if (isRoomUpdatedInOneOf) {
        return true;
      }
    }

    // check in allOf
    return this.getAllOf() != null &&  this.getAllOf().stream().anyMatch(event -> event.getRoomUpdated() != null);
  }

  public boolean isRoomReactivated() {
    if (this.roomReactivated != null) {
      return true;
    }

    if (this.getOneOf() != null) {
      boolean isRoomReactivatedInOneOf = this.getOneOf().stream().anyMatch(event -> event.getRoomReactivated() != null);
      if (isRoomReactivatedInOneOf) {
        return true;
      }
    }

    // check in allOf
    return this.getAllOf() != null &&  this.getAllOf().stream().anyMatch(event -> event.getRoomReactivated() != null);
  }

  public boolean isRoomDeactivated() {
    if (this.roomDeactivated != null) {
      return true;
    }

    if (this.getOneOf() != null) {
      boolean isRoomDeactivatedInOneOf = this.getOneOf().stream().anyMatch(event -> event.getRoomDeactivated() != null);
      if (isRoomDeactivatedInOneOf) {
        return true;
      }
    }

    // check in allOf
    return this.getAllOf() != null &&  this.getAllOf().stream().anyMatch(event -> event.getRoomDeactivated() != null);
  }

  public boolean isUserJoinedRoom() {
    if (this.userJoinedRoom != null) {
      return true;
    }

    if (this.getOneOf() != null) {
      boolean isUserJoinedRoomInOneOf = this.getOneOf().stream().anyMatch(event -> event.getUserJoinedRoom() != null);
      if (isUserJoinedRoomInOneOf) {
        return true;
      }
    }

    // check in allOf
    return this.getAllOf() != null &&  this.getAllOf().stream().anyMatch(event -> event.getUserJoinedRoom() != null);
  }

  public boolean isUserLeftRoom() {
    if (this.userLeftRoom != null) {
      return true;
    }

    if (this.getOneOf() != null) {
      boolean isUserLeftRoomInOneOf = this.getOneOf().stream().anyMatch(event -> event.getUserLeftRoom() != null);
      if (isUserLeftRoomInOneOf) {
        return true;
      }
    }

    // check in allOf
    return this.getAllOf() != null &&  this.getAllOf().stream().anyMatch(event -> event.getUserLeftRoom() != null);
  }

  public boolean isRoomMemberDemotedFromOwner() {
    if (this.roomMemberDemotedFromOwner != null) {
      return true;
    }

    if (this.getOneOf() != null) {
      boolean isRoomMemberDemotedFromOwnerInOneOf =
          this.getOneOf().stream().anyMatch(event -> event.getRoomMemberDemotedFromOwner() != null);
      if (isRoomMemberDemotedFromOwnerInOneOf) {
        return true;
      }
    }

    // check in allOf
    return this.getAllOf() != null && this.getAllOf()
        .stream()
        .anyMatch(event -> event.getRoomMemberDemotedFromOwner() != null);
  }

  public boolean isRoomMemberPromotedToOwner() {
    if (this.roomMemberPromotedToOwner != null) {
      return true;
    }

    if (this.getOneOf() != null) {
      boolean isRoomMemberPromotedToOwnerInOneOf =
          this.getOneOf().stream().anyMatch(event -> event.getRoomMemberPromotedToOwner() != null);
      if (isRoomMemberPromotedToOwnerInOneOf) {
        return true;
      }
    }

    // check in allOf
    return this.getAllOf() != null && this.getAllOf()
        .stream()
        .anyMatch(event -> event.getRoomMemberPromotedToOwner() != null);
  }

  public boolean isMessageSuppressed() {
    if (this.messageSuppressed != null) {
      return true;
    }

    if (this.getOneOf() != null) {
      boolean isMessageSuppressedInOneOf =
          this.getOneOf().stream().anyMatch(event -> event.getMessageSuppressed() != null);
      if (isMessageSuppressedInOneOf) {
        return true;
      }
    }

    // check in allOf
    return this.getAllOf() != null && this.getAllOf().stream().anyMatch(event -> event.getMessageSuppressed() != null);
  }

  public boolean isPostShared() {
    if (this.postShared != null) {
      return true;
    }

    if (this.getOneOf() != null) {
      boolean isPostSharedInOneOf =
          this.getOneOf().stream().anyMatch(event -> event.getPostShared() != null);
      if (isPostSharedInOneOf) {
        return true;
      }
    }

    // check in allOf
    return this.getAllOf() != null &&  this.getAllOf().stream().anyMatch(event -> event.getPostShared() != null);
  }

  public boolean isImCreated() {
    if (this.imCreated != null) {
      return true;
    }

    if (this.getOneOf() != null) {
      boolean isImCreatedInOneOf =
          this.getOneOf().stream().anyMatch(event -> event.getImCreated() != null);
      if (isImCreatedInOneOf) {
        return true;
      }
    }

    // check in allOf
    return this.getAllOf() != null &&  this.getAllOf().stream().anyMatch(event -> event.getImCreated() != null);
  }

  public boolean isUserRequestedJoinRoom() {
    if (this.userRequestedJoinRoom != null) {
      return true;
    }

    if (this.getOneOf() != null) {
      boolean isUserRequestedJoinRoomInOneOf =
          this.getOneOf().stream().anyMatch(event -> event.getUserRequestedJoinRoom() != null);
      if (isUserRequestedJoinRoomInOneOf) {
        return true;
      }
    }

    // check in allOf
    return this.getAllOf() != null && this.getAllOf()
        .stream()
        .anyMatch(event -> event.getUserRequestedJoinRoom() != null);
  }

  public boolean isConnectionRequested() {
    if (this.connectionRequested != null) {
      return true;
    }

    if (this.getOneOf() != null) {
      boolean isConnectionRequestedInOneOf =
          this.getOneOf().stream().anyMatch(event -> event.getConnectionRequested() != null);
      if (isConnectionRequestedInOneOf) {
        return true;
      }
    }

    // check in allOf
    return this.getAllOf() != null && this.getAllOf()
        .stream()
        .anyMatch(event -> event.getConnectionRequested() != null);
  }

  public boolean isConnectionAccepted() {
    if (this.connectionAccepted != null) {
      return true;
    }

    if (this.getOneOf() != null) {
      boolean isConnectionAcceptedInOneOf =
          this.getOneOf().stream().anyMatch(event -> event.getConnectionAccepted() != null);
      if (isConnectionAcceptedInOneOf) {
        return true;
      }
    }

    // check in allOf
    return this.getAllOf() != null && this.getAllOf().stream().anyMatch(event -> event.getConnectionAccepted() != null);
  }

  public boolean isRequestReceived() {
    if (this.requestReceived != null) {
      return true;
    }

    if (this.getOneOf() != null) {
      boolean isRequestReceivedInOneOf =
          this.getOneOf().stream().anyMatch(event -> event.getRequestReceived() != null);
      if (isRequestReceivedInOneOf) {
        return true;
      }
    }

    // check in allOf
    return this.getAllOf() != null &&  this.getAllOf().stream().anyMatch(event -> event.getRequestReceived() != null);
  }*/
}
