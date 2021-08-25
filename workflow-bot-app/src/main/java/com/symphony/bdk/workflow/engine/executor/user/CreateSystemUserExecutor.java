package com.symphony.bdk.workflow.engine.executor.user;

import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.user.CreateSystemUser;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateSystemUserExecutor implements ActivityExecutor<CreateSystemUser> {

  @Override
  public void execute(ActivityExecutorContext<CreateSystemUser> context) {
    // since we are calling the same API, delegate to CreateUserExecutor
    new CreateUserExecutor().doExecute(context);
  }
}
