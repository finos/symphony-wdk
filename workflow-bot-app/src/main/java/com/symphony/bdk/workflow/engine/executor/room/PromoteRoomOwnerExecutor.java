package com.symphony.bdk.workflow.engine.executor.room;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.room.PromoteRoomOwner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PromoteRoomOwnerExecutor implements ActivityExecutor<PromoteRoomOwner> {

  @Override
  public void execute(ActivityExecutorContext<PromoteRoomOwner> execution) {
    PromoteRoomOwner promoteRoomOwner = execution.getActivity();

    if (this.isObo(promoteRoomOwner)) {
      this.doOboWithCache(execution, promoteRoomOwner);
    } else {
      for (Long uid : promoteRoomOwner.getUserIds()) {
        log.debug("Demote owner {} for room {}", uid, promoteRoomOwner.getStreamId());
        execution.bdk().streams().promoteUserToRoomOwner(uid, promoteRoomOwner.getStreamId());
      }
    }
  }

  private boolean isObo(PromoteRoomOwner activity) {
    return activity.getObo() != null && (activity.getObo().getUsername() != null
        || activity.getObo().getUserId() != null);
  }

  private void doOboWithCache(ActivityExecutorContext<PromoteRoomOwner> execution, PromoteRoomOwner promoteRoomOwner) {
    AuthSession authSession;
    if (promoteRoomOwner.getObo().getUsername() != null) {
      authSession = execution.bdk().obo(promoteRoomOwner.getObo().getUsername());
    } else {
      authSession = execution.bdk().obo(promoteRoomOwner.getObo().getUserId());
    }

    for (Long uid : promoteRoomOwner.getUserIds()) {
      log.debug("Demote owner {} for room {}", uid, promoteRoomOwner.getStreamId());
      execution.bdk().obo(authSession).streams().promoteUserToRoomOwner(uid, promoteRoomOwner.getStreamId());
    }
  }

}
