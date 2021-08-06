package com.symphony.bdk.workflow.engine.executor;

import com.symphony.bdk.workflow.swadl.v1.activity.room.RemoveRoomMember;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemoveRoomMemberExecutor implements ActivityExecutor<RemoveRoomMember> {

  @Override
  public void execute(ActivityExecutorContext<RemoveRoomMember> execution) {
    RemoveRoomMember removeRoomMember = execution.getActivity();

    for (String uid : removeRoomMember.getUids()) {
      log.debug("Remove member {} from room {}", uid, removeRoomMember.getStreamId());
      execution.streams().removeMemberFromRoom(Long.valueOf(uid), removeRoomMember.getStreamId());
    }
  }

}
