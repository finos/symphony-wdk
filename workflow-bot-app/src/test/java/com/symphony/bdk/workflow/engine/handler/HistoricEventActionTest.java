package com.symphony.bdk.workflow.engine.handler;

import com.symphony.bdk.workflow.engine.handler.audit.AuditTrailLogAction;
import com.symphony.bdk.workflow.engine.handler.variable.WorkflowEventVariableAction;

import org.camunda.bpm.engine.impl.history.event.HistoryEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class HistoricEventActionTest {

  @Mock
  HistoricEventActionExecutor historicEventActionExecutor;

  @Mock
  AuditTrailLogAction auditTrailLogAction;

  @Mock
  WorkflowEventVariableAction workflowEventVariableAction;

  @InjectMocks
  HistoricEventHandler historicEventHandler;

  @Test
  void testHandleEvents() {
    HistoryEvent historyEvent1 = new HistoryEvent();
    HistoryEvent historyEvent2 = new HistoryEvent();
    doNothing().when(historicEventActionExecutor)
        .executeAction(any(HistoricEventAction.class), any(HistoryEvent.class));

    historicEventHandler.handleEvents(Arrays.asList(historyEvent1, historyEvent2));

    verify(historicEventActionExecutor).executeAction(eq(auditTrailLogAction), eq(historyEvent1));
    verify(historicEventActionExecutor).executeAction(eq(auditTrailLogAction), eq(historyEvent2));
    verify(historicEventActionExecutor).executeAction(eq(workflowEventVariableAction), eq(historyEvent1));
    verify(historicEventActionExecutor).executeAction(eq(workflowEventVariableAction), eq(historyEvent2));
  }

  @Test
  void testHandleEvent() {
    HistoryEvent historyEvent = new HistoryEvent();
    doNothing().when(historicEventActionExecutor)
        .executeAction(any(HistoricEventAction.class), any(HistoryEvent.class));

    historicEventHandler.handleEvent(historyEvent);

    verify(historicEventActionExecutor).executeAction(eq(auditTrailLogAction), eq(historyEvent));
    verify(historicEventActionExecutor).executeAction(eq(workflowEventVariableAction), eq(historyEvent));
  }
}
