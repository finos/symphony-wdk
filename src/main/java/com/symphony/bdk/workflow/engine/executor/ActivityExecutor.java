package com.symphony.bdk.workflow.engine.executor;

public interface ActivityExecutor<T> {

  void execute(ActivityExecutorContext<T> context);
}
