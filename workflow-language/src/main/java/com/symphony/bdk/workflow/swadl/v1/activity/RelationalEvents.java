package com.symphony.bdk.workflow.swadl.v1.activity;

import com.symphony.bdk.workflow.swadl.v1.Event;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
public class RelationalEvents {
  private final List<Event> events;
  private final boolean exclusive;

  public boolean isEmpty() {
    return events == null || events.isEmpty();
  }
}
