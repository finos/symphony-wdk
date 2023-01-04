package com.symphony.bdk.workflow.engine.handler;

import org.camunda.bpm.engine.impl.history.event.HistoryEvent;
import org.springframework.stereotype.Component;

@Component
public class HistoricEventActionExecutor {

  public void executeAction(HistoricEventAction historicEventAction, HistoryEvent historyEvent) {
    historicEventAction.execute(historyEvent);
  }
}
