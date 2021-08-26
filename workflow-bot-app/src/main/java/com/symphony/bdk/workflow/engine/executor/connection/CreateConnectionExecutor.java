package com.symphony.bdk.workflow.engine.executor.connection;

import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.connection.CreateConnection;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateConnectionExecutor implements ActivityExecutor<CreateConnection> {

  private static final String OUTPUT_CONNECTION_KEY = "connection";

  @Override
  public void execute(ActivityExecutorContext<CreateConnection> context) {
    CreateConnection activity = context.getActivity();
    context.setOutputVariable(OUTPUT_CONNECTION_KEY,
        context.bdk().connections().createConnection(Long.parseLong(activity.getUserId())));
  }
}
