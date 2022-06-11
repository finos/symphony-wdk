package com.symphony.bdk.workflow.engine.executor.room;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.room.AddRoomMember;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AddRoomMemberExecutor implements ActivityExecutor<AddRoomMember> {

  @Override
  public void execute(ActivityExecutorContext<AddRoomMember> execution) {
    AddRoomMember addRoomMember = execution.getActivity();

    if (this.isObo(addRoomMember)) {
      this.doOboWithCache(execution, addRoomMember);
    } else {
      for (Long uid : addRoomMember.getUserIds()) {
        log.debug("Add user {} to room {}", uid, addRoomMember.getStreamId());
        execution.bdk().streams().addMemberToRoom(uid, addRoomMember.getStreamId());
      }
    }
  }

  private boolean isObo(AddRoomMember activity) {
    return activity.getOnBehalfOf() != null && (activity.getOnBehalfOf().getUsername() != null
        || activity.getOnBehalfOf().getUserId() != null);
  }

  private void doOboWithCache(ActivityExecutorContext<AddRoomMember> execution, AddRoomMember addRoomMember) {
    AuthSession authSession;
    if (addRoomMember.getOnBehalfOf().getUsername() != null) {
      authSession = execution.bdk().obo(addRoomMember.getOnBehalfOf().getUsername());
    } else {
      authSession = execution.bdk().obo(addRoomMember.getOnBehalfOf().getUserId());
    }

    for (Long uid : addRoomMember.getUserIds()) {
      log.debug("Add user {} to room {}", uid, addRoomMember.getStreamId());
      execution.bdk().obo(authSession).streams().addMemberToRoom(uid, addRoomMember.getStreamId());
    }
  }

}
