package com.symphony.bdk.workflow.engine.executor.room;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.gen.api.model.V3RoomDetail;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.obo.OboExecutor;
import com.symphony.bdk.workflow.swadl.v1.activity.room.GetRoom;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetRoomExecutor extends OboExecutor<GetRoom, V3RoomDetail>
    implements ActivityExecutor<GetRoom> {

  private static final String OUTPUTS_ROOM_KEY = "room";

  @Override
  public void execute(ActivityExecutorContext<GetRoom> execution) {
    String streamId = execution.getActivity().getStreamId();
    log.debug("Getting room {}", streamId);

    V3RoomDetail roomInfo;
    if (this.isObo(execution.getActivity())) {
      roomInfo = this.doOboWithCache(execution);
    } else {
      roomInfo = execution.bdk().streams().getRoomInfo(streamId);
    }

    execution.setOutputVariable(OUTPUTS_ROOM_KEY, roomInfo);
  }

  @Override
  protected V3RoomDetail doOboWithCache(ActivityExecutorContext<GetRoom> execution) {
    AuthSession authSession = this.getOboAuthSession(execution);
    return execution.bdk().obo(authSession).streams().getRoomInfo(execution.getActivity().getStreamId());
  }

}
