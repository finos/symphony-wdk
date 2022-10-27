package com.symphony.bdk.workflow.engine.executor.user;

import com.symphony.bdk.workflow.engine.executor.AbstractActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.user.UpdateSystemUser;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateSystemUserExecutor extends AbstractActivityExecutor<UpdateSystemUser>
    implements ActivityExecutor<UpdateSystemUser> {

  @Override
  public void execute(ActivityExecutorContext<UpdateSystemUser> context) {
    // since we are calling the same API, delegate to UpdateUserExecutor
    new UpdateUserExecutor().doExecute(context);
  }
}
