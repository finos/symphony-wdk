package com.symphony.bdk.workflow.engine.executor.room;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.obo.OboExecutor;
import com.symphony.bdk.workflow.swadl.v1.activity.room.PromoteRoomOwner;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;

@Slf4j
public class PromoteRoomOwnerExecutor extends OboExecutor<PromoteRoomOwner, Void>
    implements ActivityExecutor<PromoteRoomOwner> {

  @Override
  public void execute(ActivityExecutorContext<PromoteRoomOwner> execution) {
    PromoteRoomOwner promoteRoomOwner = execution.getActivity();

    if (this.isObo(promoteRoomOwner)) {
      this.doOboWithCache(execution);
    } else {
      for (Long uid : promoteRoomOwner.getUserIds()) {
        log.debug("Demote owner {} for room {}", uid, promoteRoomOwner.getStreamId());
        execution.bdk().streams().promoteUserToRoomOwner(uid, promoteRoomOwner.getStreamId());
      }
    }
  }

  @Override
  protected Void doOboWithCache(ActivityExecutorContext<PromoteRoomOwner> execution) {
    PromoteRoomOwner promoteRoomOwner = execution.getActivity();
    AuthSession authSession = this.getOboAuthSession(execution);

    for (Long uid : promoteRoomOwner.getUserIds()) {
      log.debug("Demote owner {} for room {}", uid, promoteRoomOwner.getStreamId());
      execution.bdk().obo(authSession).streams().promoteUserToRoomOwner(uid, promoteRoomOwner.getStreamId());
    }

    return null;
  }

}
