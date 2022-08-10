package com.symphony.bdk.workflow.engine.executor.connection;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.connection.RemoveConnection;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemoveConnectionExecutor implements ActivityExecutor<RemoveConnection> {

  @Override
  public void execute(ActivityExecutorContext<RemoveConnection> context) {
    RemoveConnection activity = context.getActivity();

    if (this.isObo(activity)) {
      this.doOboWithCache(context);
    } else {
      context.bdk().connections().removeConnection(Long.parseLong(activity.getUserId()));
    }
  }

  private boolean isObo(RemoveConnection activity) {
    return activity.getObo() != null && (activity.getObo().getUsername() != null
        || activity.getObo().getUserId() != null);
  }

  private void doOboWithCache(ActivityExecutorContext<RemoveConnection> execution) {
    RemoveConnection activity = execution.getActivity();

    AuthSession authSession;
    if (activity.getObo().getUsername() != null) {
      authSession = execution.bdk().obo(activity.getObo().getUsername());
    } else {
      authSession = execution.bdk().obo(activity.getObo().getUserId());
    }

    execution.bdk().obo(authSession).connections().removeConnection(Long.parseLong(activity.getUserId()));
  }
}
