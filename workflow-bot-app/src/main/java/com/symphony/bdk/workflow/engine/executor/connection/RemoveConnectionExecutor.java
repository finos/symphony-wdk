package com.symphony.bdk.workflow.engine.executor.connection;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.obo.OboExecutor;
import com.symphony.bdk.workflow.swadl.v1.activity.connection.RemoveConnection;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemoveConnectionExecutor extends OboExecutor<RemoveConnection, Void>
    implements ActivityExecutor<RemoveConnection> {

  @Override
  public void execute(ActivityExecutorContext<RemoveConnection> context) {
    RemoveConnection activity = context.getActivity();

    if (this.isObo(activity)) {
      this.doOboWithCache(context);
    } else {
      context.bdk().connections().removeConnection(Long.parseLong(activity.getUserId()));
    }
  }


  protected Void doOboWithCache(ActivityExecutorContext<RemoveConnection> execution) {
    RemoveConnection activity = execution.getActivity();
    AuthSession authSession = this.getOboAuthSession(execution);

    execution.bdk().obo(authSession).connections().removeConnection(Long.parseLong(activity.getUserId()));
    return null;
  }
}
