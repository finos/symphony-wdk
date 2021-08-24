package com.symphony.bdk.workflow.engine.executor.room;

import com.symphony.bdk.gen.api.model.MemberInfo;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.room.GetRoomMembers;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class GetRoomMembersExecutor implements ActivityExecutor<GetRoomMembers> {

  private static final String OUTPUTS_MEMBERS_KEY = "members";

  @Override
  public void execute(ActivityExecutorContext<GetRoomMembers> execution) {
    String streamId = execution.getActivity().getStreamId();
    log.debug("Getting room members for stream {}", streamId);
    List<MemberInfo> roomInfo = execution.streams().listRoomMembers(streamId);
    execution.setOutputVariable(OUTPUTS_MEMBERS_KEY, roomInfo);
  }

}
