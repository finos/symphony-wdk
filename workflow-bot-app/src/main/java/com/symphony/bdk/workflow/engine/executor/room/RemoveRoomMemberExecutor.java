package com.symphony.bdk.workflow.engine.executor.room;

import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.room.RemoveRoomMember;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemoveRoomMemberExecutor implements ActivityExecutor<RemoveRoomMember> {

  @Override
  public void execute(ActivityExecutorContext<RemoveRoomMember> execution) {
    RemoveRoomMember removeRoomMember = execution.getActivity();

    for (String uid : removeRoomMember.getUids()) {
      log.debug("Remove member {} from room {}", uid, removeRoomMember.getStreamId());
      execution.bdk().streams().removeMemberFromRoom(Long.valueOf(uid), removeRoomMember.getStreamId());
    }
  }

}
