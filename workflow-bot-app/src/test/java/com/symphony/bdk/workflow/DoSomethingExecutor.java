package com.symphony.bdk.workflow;

import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DoSomethingExecutor implements ActivityExecutor<DoSomething> {

  @Override
  public void execute(ActivityExecutorContext<DoSomething> context) {
    context.messages().send("123", context.getActivity().getMyParameter());
  }
}
