package com.symphony.bdk.workflow.engine.executor;

import com.symphony.bdk.workflow.swadl.v1.activity.room.DemoteRoomOwner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DemoteRoomOwnerExecutor implements ActivityExecutor<DemoteRoomOwner> {

  @Override
  public void execute(ActivityExecutorContext<DemoteRoomOwner> execution) {
    DemoteRoomOwner demoteRoomOwner = execution.getActivity();

    for (String uid : demoteRoomOwner.getUids()) {
      log.debug("Demote owner {} for room {}", uid, demoteRoomOwner.getStreamId());
      execution.streams().demoteUserToRoomParticipant(Long.valueOf(uid), demoteRoomOwner.getStreamId());
    }
  }

}
