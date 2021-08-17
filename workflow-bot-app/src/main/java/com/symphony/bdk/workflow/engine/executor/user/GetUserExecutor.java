package com.symphony.bdk.workflow.engine.executor.user;

import com.symphony.bdk.gen.api.model.V2UserDetail;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.user.GetUser;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetUserExecutor implements ActivityExecutor<GetUser> {

  private static final String OUTPUT_USER_KEY = "user";

  @Override
  public void execute(ActivityExecutorContext<GetUser> context) {
    Long userId = Long.valueOf(context.getActivity().getUserId());

    log.debug("Getting user {}", userId);
    V2UserDetail userDetail = context.users().getUserDetail(userId);

    context.setOutputVariable(OUTPUT_USER_KEY, userDetail);
  }

}
