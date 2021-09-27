package com.symphony.bdk.workflow.engine.executor.room;

import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.room.AddRoomMember;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddRoomMemberExecutor implements ActivityExecutor<AddRoomMember> {

  @Override
  public void execute(ActivityExecutorContext<AddRoomMember> execution) {
    AddRoomMember addRoomMember = execution.getActivity();

    for (Number uid : addRoomMember.getUserIds().get()) {
      log.debug("Add user {} to room {}", uid, addRoomMember.getStreamId());
      execution.bdk().streams().addMemberToRoom(uid.longValue(), addRoomMember.getStreamId());
    }
  }

}
