package com.symphony.bdk.workflow.engine.camunda.audit;

import com.symphony.bdk.workflow.engine.handler.audit.AuditTrailLogAction;
import com.symphony.bdk.workflow.swadl.v1.activity.ExecuteScript;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

/**
 * To audit trail script task executions.
 */
@Component
public class ScriptTaskAuditListener implements ExecutionListener {

  private final AuditTrailLogAction auditTrailLogger;

  public ScriptTaskAuditListener(AuditTrailLogAction auditTrailLogger) {
    this.auditTrailLogger = auditTrailLogger;
  }

  @Override
  public void notify(DelegateExecution execution) {
    auditTrailLogger.execute(execution, ExecuteScript.class.getSimpleName());
  }
}
