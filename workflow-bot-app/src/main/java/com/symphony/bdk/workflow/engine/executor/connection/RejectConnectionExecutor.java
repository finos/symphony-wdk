package com.symphony.bdk.workflow.engine.executor.connection;

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
    UserConnection connection = context.bdk().connections().rejectConnection(Long.parseLong(activity.getUserId()));
    context.setOutputVariable(OUTPUT_CONNECTION_KEY, connection);
  }
}
