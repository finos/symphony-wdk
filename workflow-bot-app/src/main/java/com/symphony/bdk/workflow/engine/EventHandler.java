package com.symphony.bdk.workflow.engine;

import org.camunda.bpm.engine.impl.history.event.HistoryEvent;
import org.camunda.bpm.engine.impl.history.handler.HistoryEventHandler;

import java.util.List;

public interface EventHandler extends HistoryEventHandler {

  @Override
  default void handleEvents(List<HistoryEvent> historyEvents) {
    historyEvents.forEach(this::handleEvent);
  }

}
