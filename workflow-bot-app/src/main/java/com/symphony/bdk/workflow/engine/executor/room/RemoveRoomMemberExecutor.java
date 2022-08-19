package com.symphony.bdk.workflow.engine.executor.room;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.obo.OboExecutor;
import com.symphony.bdk.workflow.swadl.v1.activity.room.RemoveRoomMember;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemoveRoomMemberExecutor extends OboExecutor<RemoveRoomMember, Void>
    implements ActivityExecutor<RemoveRoomMember> {

  @Override
  public void execute(ActivityExecutorContext<RemoveRoomMember> execution) {
    RemoveRoomMember removeRoomMember = execution.getActivity();

    if (this.isObo(removeRoomMember)) {
      this.doOboWithCache(execution);
    } else {
      for (Long uid : removeRoomMember.getUserIds()) {
        log.debug("Remove member {} from room {}", uid, removeRoomMember.getStreamId());
        execution.bdk().streams().removeMemberFromRoom(uid, removeRoomMember.getStreamId());
      }
    }
  }

  @Override
  protected Void doOboWithCache(ActivityExecutorContext<RemoveRoomMember> execution) {
    RemoveRoomMember removeRoomMember = execution.getActivity();
    AuthSession authSession = this.getOboAuthSession(execution);

    for (Long uid : removeRoomMember.getUserIds()) {
      log.debug("Remove member {} from room {}", uid, removeRoomMember.getStreamId());
      execution.bdk().obo(authSession).streams().removeMemberFromRoom(uid, removeRoomMember.getStreamId());
    }

    return null;
  }

}
