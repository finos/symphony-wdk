package com.symphony.bdk.workflow.engine.executor.room;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.obo.OboExecutor;
import com.symphony.bdk.workflow.swadl.v1.activity.room.AddRoomMember;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AddRoomMemberExecutor extends OboExecutor<AddRoomMember, Void>
    implements ActivityExecutor<AddRoomMember> {

  @Override
  public void execute(ActivityExecutorContext<AddRoomMember> execution) {
    AddRoomMember addRoomMember = execution.getActivity();

    if (this.isObo(addRoomMember)) {
      this.doOboWithCache(execution);
    } else {
      for (Long uid : addRoomMember.getUserIds()) {
        log.debug("Add user {} to room {}", uid, addRoomMember.getStreamId());
        execution.bdk().streams().addMemberToRoom(uid, addRoomMember.getStreamId());
      }
    }
  }

  @Override
  protected Void doOboWithCache(ActivityExecutorContext<AddRoomMember> execution) {
    AddRoomMember addRoomMember = execution.getActivity();
    AuthSession authSession = this.getOboAuthSession(execution);

    for (Long uid : addRoomMember.getUserIds()) {
      log.debug("Add user {} to room {}", uid, addRoomMember.getStreamId());
      execution.bdk().obo(authSession).streams().addMemberToRoom(uid, addRoomMember.getStreamId());
    }

    return null;
  }

}
