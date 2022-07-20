package com.symphony.bdk.workflow.engine.executor.room;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.room.RemoveRoomMember;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemoveRoomMemberExecutor implements ActivityExecutor<RemoveRoomMember> {

  @Override
  public void execute(ActivityExecutorContext<RemoveRoomMember> execution) {
    RemoveRoomMember removeRoomMember = execution.getActivity();

    if (this.isObo(removeRoomMember)) {
      this.doOboWithCache(execution, removeRoomMember);
    } else {
      for (Long uid : removeRoomMember.getUserIds()) {
        log.debug("Remove member {} from room {}", uid, removeRoomMember.getStreamId());
        execution.bdk().streams().removeMemberFromRoom(uid, removeRoomMember.getStreamId());
      }
    }
  }

  private boolean isObo(RemoveRoomMember activity) {
    return activity.getObo() != null && (activity.getObo().getUsername() != null
        || activity.getObo().getUserId() != null);
  }

  private void doOboWithCache(ActivityExecutorContext<RemoveRoomMember> execution, RemoveRoomMember removeRoomMember) {
    AuthSession authSession;
    if (removeRoomMember.getObo().getUsername() != null) {
      authSession = execution.bdk().obo(removeRoomMember.getObo().getUsername());
    } else {
      authSession = execution.bdk().obo(removeRoomMember.getObo().getUserId());
    }

    for (Long uid : removeRoomMember.getUserIds()) {
      log.debug("Remove member {} from room {}", uid, removeRoomMember.getStreamId());
      execution.bdk().obo(authSession).streams().removeMemberFromRoom(uid, removeRoomMember.getStreamId());
    }
  }

}
