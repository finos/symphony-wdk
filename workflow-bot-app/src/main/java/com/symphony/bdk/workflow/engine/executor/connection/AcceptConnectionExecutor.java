package com.symphony.bdk.workflow.engine.executor.connection;

import com.symphony.bdk.gen.api.model.UserConnection;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.connection.AcceptConnection;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AcceptConnectionExecutor implements ActivityExecutor<AcceptConnection> {

  private static final String OUTPUT_CONNECTION_KEY = "connection";

  @Override
  public void execute(ActivityExecutorContext<AcceptConnection> context) {
    AcceptConnection acceptConnection = context.getActivity();
    UserConnection connection =
        context.bdk().connections().acceptConnection(Long.parseLong(acceptConnection.getUserId()));
    context.setOutputVariable(OUTPUT_CONNECTION_KEY, connection);
  }
}
