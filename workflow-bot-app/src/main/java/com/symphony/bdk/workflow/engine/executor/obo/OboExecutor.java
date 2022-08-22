package com.symphony.bdk.workflow.engine.executor.obo;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.OboActivity;

public abstract class OboExecutor<T extends OboActivity, R> {

  protected boolean isObo(T activity) {
    return activity.getObo() != null && (activity.getObo().getUsername() != null
        || activity.getObo().getUserId() != null);
  }

  protected AuthSession getOboAuthSession(ActivityExecutorContext<T> execution) {
    T activity = execution.getActivity();

    if (activity.getObo().getUsername() != null) {
      return execution.bdk().obo(activity.getObo().getUsername());
    } else {
      return execution.bdk().obo(activity.getObo().getUserId());
    }
  }

  protected abstract R doOboWithCache(ActivityExecutorContext<T> execution) throws Exception;
}
