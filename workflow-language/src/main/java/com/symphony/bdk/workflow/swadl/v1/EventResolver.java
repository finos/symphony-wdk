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
    return this.event.getAllOf() != null &&  this.event.getAllOf().stream().anyMatch(e -> e.getMessageReceived() != null);
  }

  public boolean isEventOfType(String type) {
    return this.event.getEventType().equals(type) || getAllOfEventTypes().contains(type) || getOneOfEventTypes().contains(type);
  }

  public Object getEventOfType(String type) {
    switch (type) {
      case "MessageReceivedEvent":
        if (this.event.getMessageReceived() != null) {
          return this.event.getMessageReceived();
        } else if (this.getOneOfEventTypes().contains("MessageReceivedEvent")) {
          return this.event.getOneOf()
              .stream()
              .filter(e -> e.getEventType().equals("MessageReceivedEvent"))
              .findFirst()
              .get()
              .getMessageReceived();
        } else if (this.getAllOfEventTypes().contains("MessageReceivedEvent")) {
          return this.event.getAllOf()
              .stream()
              .filter(e -> e.getEventType().equals("MessageReceivedEvent"))
              .findFirst()
              .get()
              .getMessageReceived();
        }
        return null;

      default:
        return null;
    }
  }

  public List<Object> getAllEventsOfType(String type) {
    List<Object> events = new ArrayList<>();
    switch (type) {
      case "MessageReceivedEvent":
        if (this.event.getMessageReceived() != null) {
          events.add(this.event.getMessageReceived());
        }

        if (this.event.getOneOf() != null) {
          events.addAll(this.event.getOneOf()
              .stream()
              .filter(e -> e.getEventType().equals("MessageReceivedEvent"))
              .map(Event::getMessageReceived)
              .collect(Collectors.toList()));
        }

        if (this.event.getAllOf() != null) {
          events.addAll(this.event.getAllOf()
              .stream()
              .filter(e -> e.getEventType().equals("MessageReceivedEvent"))
              .map(Event::getMessageReceived)
              .collect(Collectors.toList()));
        }

        break;

      case "FormRepliedEvent":
        if (this.event.getFormReplied() != null) {
          events.add(this.event.getFormReplied());
        }

        if (this.event.getOneOf() != null) {
          events.addAll(this.event.getOneOf()
              .stream()
              .filter(e -> e.getEventType().equals("FormRepliedEvent"))
              .map(Event::getFormReplied)
              .collect(Collectors.toList()));
        }

        if (this.event.getAllOf() != null) {
          events.addAll(this.event.getAllOf()
              .stream()
              .filter(e -> e.getEventType().equals("FormRepliedEvent"))
              .map(Event::getFormReplied)
              .collect(Collectors.toList()));
        }

        break;

      default:
        break;
    }
    return events.stream().filter(Objects::nonNull).collect(Collectors.toList());
  }
}
