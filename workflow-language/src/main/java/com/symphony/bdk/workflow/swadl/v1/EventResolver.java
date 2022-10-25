package com.symphony.bdk.workflow.swadl.v1;

import com.symphony.bdk.workflow.swadl.v1.event.MessageReceivedEvent;

import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
public class EventResolver {
  private final Event event;

  public boolean isMessageReceived() {
    if (this.event.getMessageReceived() != null) {
      return true;
    }

    if (this.event.getOneOf() != null) {
      boolean isMessageReceivedInOneOf =
          this.event.getOneOf().stream().anyMatch(e -> e.getMessageReceived() != null);
      if (isMessageReceivedInOneOf) {
        return true;
      }
    }

    // check in allOf
    return this.event.getAllOf() != null &&  this.event.getAllOf().stream().anyMatch(e -> e.getMessageReceived() != null);
  }

  public boolean isEventOfType(String type) {
    return this.event.getEventType().equals(type) || getAllOfEventTypes().contains(type) || getOneOfEventTypes().contains(type);
  }

  public List<String> getAllOfEventTypes() {
    if (this.event.getAllOf() != null) {
      return this.event.getAllOf().stream().map(Event::getEventType).collect(Collectors.toList());
    }

    return Collections.emptyList();
  }

  public List<String> getOneOfEventTypes() {
    if (this.event.getOneOf() != null) {
      return this.event.getOneOf().stream().map(Event::getEventType).collect(Collectors.toList());
    }

    return Collections.emptyList();
  }

  public String getMessageReceivedEventContent() {
    if (!this.isEventOfType(MessageReceivedEvent.class.getSimpleName())) {
      return null;
    }

    if (this.event.getMessageReceived() != null) {
      return this.event.getMessageReceived().getContent();
    }

    if (this.event.getOneOf() != null) {
      Optional<Event> optional = this.event.getOneOf()
          .stream()
          .filter(e -> event.getMessageReceived() != null && !e.getMessageReceived().getContent().isBlank())
          .findFirst();

      if (optional.isPresent()) {
        return optional.get().getMessageReceived().getContent();
      }
    }

    if (this.event.getAllOf() != null) {
      Optional<Event> optional = this.event.getAllOf()
          .stream()
          .filter(e -> e.getMessageReceived() != null && !e.getMessageReceived().getContent().isBlank())
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
    return this.event.getAllOf() != null && this.event.getAllOf().stream().anyMatch(event -> event.getFormReplied() != null);
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
    return this.event.getAllOf() != null &&  this.event.getAllOf().stream().anyMatch(event -> event.getRoomCreated() != null);
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
    return this.event.getAllOf() != null &&  this.event.getAllOf().stream().anyMatch(event -> event.getRoomUpdated() != null);
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
    return this.event.getAllOf() != null &&  this.event.getAllOf().stream().anyMatch(event -> event.getRoomReactivated() != null);
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
    return this.event.getAllOf() != null &&  this.event.getAllOf().stream().anyMatch(event -> event.getRoomDeactivated() != null);
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
    return this.event.getAllOf() != null &&  this.event.getAllOf().stream().anyMatch(event -> event.getUserJoinedRoom() != null);
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
    return this.event.getAllOf() != null &&  this.event.getAllOf().stream().anyMatch(event -> event.getUserLeftRoom() != null);
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
    return this.event.getAllOf() != null && this.event.getAllOf()
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
    return this.event.getAllOf() != null && this.event.getAllOf()
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
    return this.event.getAllOf() != null && this.event.getAllOf().stream().anyMatch(event -> event.getMessageSuppressed() != null);
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
    return this.event.getAllOf() != null &&  this.event.getAllOf().stream().anyMatch(event -> event.getPostShared() != null);
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
    return this.event.getAllOf() != null &&  this.event.getAllOf().stream().anyMatch(event -> event.getImCreated() != null);
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
    return this.event.getAllOf() != null && this.event.getAllOf()
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
    return this.event.getAllOf() != null && this.event.getAllOf()
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
    return this.event.getAllOf() != null && this.event.getAllOf().stream().anyMatch(event -> event.getConnectionAccepted() != null);
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
    return this.event.getAllOf() != null &&  this.event.getAllOf().stream().anyMatch(event -> event.getRequestReceived() != null);
  }*/
}
