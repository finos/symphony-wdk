package com.symphony.bdk.workflow.engine.executor.connection;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.gen.api.model.UserConnection;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.obo.OboExecutor;
import com.symphony.bdk.workflow.swadl.v1.activity.connection.AcceptConnection;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AcceptConnectionExecutor extends OboExecutor<AcceptConnection, UserConnection>
    implements ActivityExecutor<AcceptConnection> {

  private static final String OUTPUT_CONNECTION_KEY = "connection";

  @Override
  public void execute(ActivityExecutorContext<AcceptConnection> context) {
    AcceptConnection acceptConnection = context.getActivity();
    UserConnection connection;
    if (this.isObo(acceptConnection)) {
      connection = this.doOboWithCache(context);
    } else {
      connection = context.bdk().connections().acceptConnection(Long.parseLong(acceptConnection.getUserId()));
    }

    context.setOutputVariable(OUTPUT_CONNECTION_KEY, connection);
  }

  @Override
  protected UserConnection doOboWithCache(ActivityExecutorContext<AcceptConnection> execution) {
    AcceptConnection activity = execution.getActivity();
    AuthSession authSession = this.getOboAuthSession(execution);
    return execution.bdk().obo(authSession).connections().acceptConnection(Long.parseLong(activity.getUserId()));
  }
}
