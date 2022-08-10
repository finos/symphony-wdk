package com.symphony.bdk.workflow.engine.executor.connection;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.gen.api.model.UserConnection;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.connection.RejectConnection;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RejectConnectionExecutor implements ActivityExecutor<RejectConnection> {

  private static final String OUTPUT_CONNECTION_KEY = "connection";

  @Override
  public void execute(ActivityExecutorContext<RejectConnection> context) {
    RejectConnection activity = context.getActivity();
    UserConnection connection;
    if (this.isObo(activity)) {
      connection = this.doOboWithCache(context);
    } else {
      connection = context.bdk().connections().rejectConnection(Long.parseLong(activity.getUserId()));
    }
    context.setOutputVariable(OUTPUT_CONNECTION_KEY, connection);
  }

  private boolean isObo(RejectConnection activity) {
    return activity.getObo() != null && (activity.getObo().getUsername() != null
        || activity.getObo().getUserId() != null);
  }

  private UserConnection doOboWithCache(ActivityExecutorContext<RejectConnection> execution) {
    RejectConnection activity = execution.getActivity();

    AuthSession authSession;
    if (activity.getObo().getUsername() != null) {
      authSession = execution.bdk().obo(activity.getObo().getUsername());
    } else {
      authSession = execution.bdk().obo(activity.getObo().getUserId());
    }

    return execution.bdk().obo(authSession).connections().rejectConnection(Long.parseLong(activity.getUserId()));
  }
}
