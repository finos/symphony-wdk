package com.symphony.bdk.workflow.swadl.v1.activity;

import com.symphony.bdk.workflow.swadl.v1.Event;
import com.symphony.bdk.workflow.swadl.v1.event.ActivityEvent;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@Data
@RequiredArgsConstructor
public class RelationalEvents {
  private final List<Event> events;
  private final boolean parallel;
  private String parentId;

  public boolean isEmpty() {
    return events == null || events.isEmpty();
  }

  public String getParentId() {
    if (parallel && events != null && !events.isEmpty() && parentId == null) {
      Optional<Event> optionalEvent = events.stream()
          .filter(event -> event.getActivityExpired() != null || event.getActivityCompleted() != null
              || event.getActivityFailed() != null)
          .findFirst();

      if (optionalEvent.isPresent()) {
        Event event = optionalEvent.get();
        parentId = event.getActivityCompleted() != null ? event.getActivityCompleted().getActivityId()
            : event.getActivityFailed() != null ? event.getActivityFailed().getActivityId()
                : event.getActivityExpired() != null ? event.getActivityExpired().getActivityId() : "";
        return parentId;
      }
    }
    return parentId;
  }
}
