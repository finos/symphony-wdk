package com.symphony.bdk.workflow.engine.handler;

import org.camunda.bpm.engine.impl.history.event.HistoryEvent;

@FunctionalInterface
public interface HistoricEventAction {
  void execute(HistoryEvent historyEvent);
}
