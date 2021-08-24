package com.symphony.bdk.workflow.engine.executor.room;

import com.symphony.bdk.gen.api.model.V3RoomDetail;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.room.GetRoom;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetRoomExecutor implements ActivityExecutor<GetRoom> {

  private static final String OUTPUTS_ROOM_KEY = "room";

  @Override
  public void execute(ActivityExecutorContext<GetRoom> execution) {
    String streamId = execution.getActivity().getStreamId();
    log.debug("Getting room {}", streamId);
    V3RoomDetail roomInfo = execution.streams().getRoomInfo(streamId);
    execution.setOutputVariable(OUTPUTS_ROOM_KEY, roomInfo);
  }

}
