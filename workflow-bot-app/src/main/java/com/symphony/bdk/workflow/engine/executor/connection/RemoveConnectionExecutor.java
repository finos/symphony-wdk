package com.symphony.bdk.workflow.engine.executor.connection;

import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.connection.RemoveConnection;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemoveConnectionExecutor implements ActivityExecutor<RemoveConnection> {

  @Override
  public void execute(ActivityExecutorContext<RemoveConnection> context) {
    RemoveConnection activity = context.getActivity();
    context.bdk().connections().removeConnection(Long.parseLong(activity.getUserId()));
  }
}
