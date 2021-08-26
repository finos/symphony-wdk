package com.symphony.bdk.workflow.engine.executor.connection;

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
    context.setOutputVariable(OUTPUT_CONNECTION_KEY,
        context.connections().getConnection(Long.parseLong(activity.getUserId())));
  }
}
