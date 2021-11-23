package com.symphony.bdk.workflow.engine.executor.room;

import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.room.PromoteRoomOwner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PromoteRoomOwnerExecutor implements ActivityExecutor<PromoteRoomOwner> {

  @Override
  public void execute(ActivityExecutorContext<PromoteRoomOwner> execution) {
    PromoteRoomOwner promoteRoomOwner = execution.getActivity();

    for (Long uid : promoteRoomOwner.getUserIds()) {
      log.debug("Demote owner {} for room {}", uid, promoteRoomOwner.getStreamId());
      execution.bdk().streams().promoteUserToRoomOwner(uid, promoteRoomOwner.getStreamId());
    }
  }

}
