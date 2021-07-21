package com.symphony.bdk.workflow.engine.executor;

public interface ActivityExecutor<T> {

  String EVENT = "event";

  void execute(ActivityExecutorContext<T> context);
}
