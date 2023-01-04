package com.symphony.bdk.workflow.engine.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.symphony.bdk.workflow.engine.handler.audit.AuditTrailLogAction;
import com.symphony.bdk.workflow.engine.handler.variable.WorkflowEventVariableAction;

import org.camunda.bpm.engine.impl.history.event.HistoryEvent;
import org.junit.jupiter.api.Test;

public class HistoricEventActionExecutorTest {

  @Test
  void testWorkflowEventVariableAction() {
    final HistoricEventActionExecutor historicEventActionExecutor = new HistoricEventActionExecutor();
    final HistoryEvent historyEvent = new HistoryEvent();

    final HistoricEventAction eventAction = mock(WorkflowEventVariableAction.class);
    doNothing().when(eventAction).execute(any(HistoryEvent.class));

    historicEventActionExecutor.executeAction(eventAction, historyEvent);
    verify(eventAction).execute(eq(historyEvent));
  }

  @Test
  void testAuditTrailLogAction() {
    final HistoricEventActionExecutor historicEventActionExecutor = new HistoricEventActionExecutor();
    final HistoryEvent historyEvent = new HistoryEvent();

    final HistoricEventAction eventAction = mock(AuditTrailLogAction.class);
    doNothing().when(eventAction).execute(any(HistoryEvent.class));

    historicEventActionExecutor.executeAction(eventAction, historyEvent);
    verify(eventAction).execute(eq(historyEvent));
  }

}
