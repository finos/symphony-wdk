package com.symphony.bdk.workflow.engine.executor.room;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.room.DemoteRoomOwner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DemoteRoomOwnerExecutor implements ActivityExecutor<DemoteRoomOwner> {

  @Override
  public void execute(ActivityExecutorContext<DemoteRoomOwner> execution) {
    DemoteRoomOwner demoteRoomOwner = execution.getActivity();

    if (this.isObo(demoteRoomOwner)) {
      this.doOboWithCache(execution, demoteRoomOwner);
    } else {
      for (Long uid : demoteRoomOwner.getUserIds()) {
        log.debug("Demote owner {} for room {}", uid, demoteRoomOwner.getStreamId());
        execution.bdk().streams().demoteUserToRoomParticipant(uid, demoteRoomOwner.getStreamId());
      }
    }
  }

  private boolean isObo(DemoteRoomOwner activity) {
    return activity.getObo() != null && (activity.getObo().getUsername() != null
        || activity.getObo().getUserId() != null);
  }

  private void doOboWithCache(ActivityExecutorContext<DemoteRoomOwner> execution, DemoteRoomOwner demoteRoomOwner) {
    AuthSession authSession;
    if (demoteRoomOwner.getObo().getUsername() != null) {
      authSession = execution.bdk().obo(demoteRoomOwner.getObo().getUsername());
    } else {
      authSession = execution.bdk().obo(demoteRoomOwner.getObo().getUserId());
    }

    for (Long uid : demoteRoomOwner.getUserIds()) {
      log.debug("Demote owner {} for room {}", uid, demoteRoomOwner.getStreamId());
      execution.bdk().obo(authSession).streams().demoteUserToRoomParticipant(uid, demoteRoomOwner.getStreamId());
    }
  }
}
