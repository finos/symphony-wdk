package com.symphony.bdk.workflow.engine.executor.connection;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.gen.api.model.UserConnection;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.connection.GetConnection;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetConnectionExecutor implements ActivityExecutor<GetConnection> {

  private static final String OUTPUT_CONNECTION_KEY = "connection";

  @Override
  public void execute(ActivityExecutorContext<GetConnection> context) {
    GetConnection activity = context.getActivity();
    UserConnection connection;
    if (this.isObo(activity)) {
      connection = this.doOboWithCache(context);
    } else {
      connection = context.bdk().connections().getConnection(Long.parseLong(activity.getUserId()));
    }
    context.setOutputVariable(OUTPUT_CONNECTION_KEY, connection);
  }

  private boolean isObo(GetConnection activity) {
    return activity.getObo() != null && (activity.getObo().getUsername() != null
        || activity.getObo().getUserId() != null);
  }

  private UserConnection doOboWithCache(ActivityExecutorContext<GetConnection> execution) {
    GetConnection activity = execution.getActivity();

    AuthSession authSession;
    if (activity.getObo().getUsername() != null) {
      authSession = execution.bdk().obo(activity.getObo().getUsername());
    } else {
      authSession = execution.bdk().obo(activity.getObo().getUserId());
    }

    return execution.bdk().obo(authSession).connections().getConnection(Long.parseLong(activity.getUserId()));
  }
}
