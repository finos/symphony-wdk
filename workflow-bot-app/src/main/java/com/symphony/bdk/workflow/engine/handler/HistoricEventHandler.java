package com.symphony.bdk.workflow.engine.handler;

import com.symphony.bdk.workflow.engine.handler.audit.AuditTrailLogAction;
import com.symphony.bdk.workflow.engine.handler.variable.WorkflowEventVariableAction;

import org.camunda.bpm.engine.impl.history.event.HistoryEvent;
import org.camunda.bpm.engine.impl.history.handler.HistoryEventHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HistoricEventHandler implements HistoryEventHandler {
  final HistoricEventActionExecutor historicEventActionExecutor;
  final AuditTrailLogAction auditTrailLogAction;
  final WorkflowEventVariableAction workflowEventVariableAction;

  public HistoricEventHandler(HistoricEventActionExecutor historicEventActionExecutor,
      AuditTrailLogAction auditTrailLogAction, WorkflowEventVariableAction workflowEventVariableAction) {
    this.historicEventActionExecutor = historicEventActionExecutor;
    this.auditTrailLogAction = auditTrailLogAction;
    this.workflowEventVariableAction = workflowEventVariableAction;
  }

  @Override
  public void handleEvent(HistoryEvent historyEvent) {
    this.historicEventActionExecutor.executeAction(this.auditTrailLogAction, historyEvent);
    this.historicEventActionExecutor.executeAction(this.workflowEventVariableAction, historyEvent);
  }

  @Override
  public void handleEvents(List<HistoryEvent> historyEvents) {
    historyEvents.forEach(this::handleEvent);
  }
}
