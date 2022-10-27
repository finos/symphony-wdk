package com.symphony.bdk.workflow.engine.executor.connection;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.gen.api.model.UserConnection;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.obo.OboExecutor;
import com.symphony.bdk.workflow.swadl.v1.activity.connection.RejectConnection;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RejectConnectionExecutor extends OboExecutor<RejectConnection, UserConnection>
    implements ActivityExecutor<RejectConnection> {

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

  protected UserConnection doOboWithCache(ActivityExecutorContext<RejectConnection> execution) {
    RejectConnection activity = execution.getActivity();
    AuthSession authSession = this.getOboAuthSession(execution);

    return execution.bdk().obo(authSession).connections().rejectConnection(Long.parseLong(activity.getUserId()));
  }
}
