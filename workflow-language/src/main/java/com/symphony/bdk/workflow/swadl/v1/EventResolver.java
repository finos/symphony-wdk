package com.symphony.bdk.workflow.swadl.v1;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
public class EventResolver {
  private final Event event;

  private List<String> getAllOfEventTypes() {
    if (this.event.getAllOf() != null) {
      return this.event.getAllOf().stream().map(Event::getEventType).collect(Collectors.toList());
    }

    return Collections.emptyList();
  }

  private List<String> getOneOfEventTypes() {
    if (this.event.getOneOf() != null) {
      return this.event.getOneOf().stream().map(Event::getEventType).collect(Collectors.toList());
    }

    return Collections.emptyList();
  }

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
    return this.event.getAllOf() != null && this.event.getAllOf()
        .stream()
        .anyMatch(e -> e.getMessageReceived() != null);
  }

  public boolean isEventOfType(String type) {
    return this.event.getEventType().equals(type) || getAllOfEventTypes().contains(type)
        || getOneOfEventTypes().contains(type);
  }

  public List<Object> getAllEventsOfType(String type) {
    List<Object> events = new ArrayList<>();
    B b = new B();
    switch (type) {
      case "MessageReceivedEvent":
        events.addAll(b.function(this.event, type, (e, t) -> e.getEventType().equals(t), Event::getMessageReceived));
        break;

      case "FormRepliedEvent":
        events.addAll(b.function(this.event, type, (e, t) -> e.getEventType().equals(t), Event::getFormReplied));
        break;

      case "MessageSuppressedEvent":
        events.addAll(b.function(this.event, type, (e, t) -> e.getEventType().equals(t), Event::getMessageSuppressed));
        break;

      case "PostSharedEvent":
        events.addAll(b.function(this.event, type, (e, t) -> e.getEventType().equals(t), Event::getPostShared));
        break;

      case "ImCreatedEvent":
        events.addAll(b.function(this.event, type, (e, t) -> e.getEventType().equals(t), Event::getImCreated));
        break;

      case "RoomCreatedEvent":
        events.addAll(b.function(this.event, type, (e, t) -> e.getEventType().equals(t), Event::getRoomCreated));
        break;

      case "RoomUpdatedEvent":
        events.addAll(b.function(this.event, type, (e, t) -> e.getEventType().equals(t), Event::getRoomUpdated));
        break;

      case "RoomDeactivatedEvent":
        events.addAll(b.function(this.event, type, (e, t) -> e.getEventType().equals(t), Event::getRoomDeactivated));
        break;

      case "RoomReactivatedEvent":
        events.addAll(b.function(this.event, type, (e, t) -> e.getEventType().equals(t), Event::getRoomReactivated));
        break;

      case "RoomMemberPromotedToOwnerEvent":
        events.addAll(
            b.function(this.event, type, (e, t) -> e.getEventType().equals(t), Event::getRoomMemberPromotedToOwner));
        break;

      case "RoomMemberDemotedFromOwnerEvent":
        events.addAll(
            b.function(this.event, type, (e, t) -> e.getEventType().equals(t), Event::getRoomMemberDemotedFromOwner));
        break;

      case "UserJoinedRoomEvent":
        events.addAll(b.function(this.event, type, (e, t) -> e.getEventType().equals(t), Event::getUserJoinedRoom));
        break;

      case "UserLeftRoomEvent":
        events.addAll(b.function(this.event, type, (e, t) -> e.getEventType().equals(t), Event::getUserLeftRoom));
        break;

      case "UserRequestedToJoinRoomEvent":
        events.addAll(
            b.function(this.event, type, (e, t) -> e.getEventType().equals(t), Event::getUserRequestedJoinRoom));
        break;

      case "ConnectionRequestedEvent":
        events.addAll(
            b.function(this.event, type, (e, t) -> e.getEventType().equals(t), Event::getConnectionRequested));
        break;

      case "ConnectionAcceptedEvent":
        events.addAll(b.function(this.event, type, (e, t) -> e.getEventType().equals(t), Event::getConnectionAccepted));
        break;

      case "RequestReceivedEvent":
        events.addAll(b.function(this.event, type, (e, t) -> e.getEventType().equals(t), Event::getRequestReceived));
        break;

      default:
        break;
    }

    return events.stream().filter(Objects::nonNull).collect(Collectors.toList());
  }

  @FunctionalInterface
  interface I {
    boolean check(Event e, String type);
  }


  @FunctionalInterface
  interface J {
    Object getEvent(Event e);
  }


  class B {
    public List<Object> function(Event event, String type, I myInterface, J myJnterface) {
      List<Object> events = new ArrayList<>();
      if (myInterface.check(event, type)) {
        events.add(myJnterface.getEvent(event));
      }

      if (event.getOneOf() != null) {
        events.addAll(event.getOneOf()
            .stream()
            .filter(e -> myInterface.check(e, type))
            .map(myJnterface::getEvent)
            .collect(Collectors.toList()));
      }

      if (event.getAllOf() != null) {
        events.addAll(event.getAllOf()
            .stream()
            .filter(e -> myInterface.check(e, type))
            .map(myJnterface::getEvent)
            .collect(Collectors.toList()));
      }

      return events;
    }
  }
}
