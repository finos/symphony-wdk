package com.symphony.bdk.workflow.engine.executor.room;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.obo.OboExecutor;
import com.symphony.bdk.workflow.swadl.v1.activity.room.DemoteRoomOwner;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;

@Slf4j
public class DemoteRoomOwnerExecutor extends OboExecutor<DemoteRoomOwner, Void>
    implements ActivityExecutor<DemoteRoomOwner> {

  protected DemoteRoomOwnerExecutor(RuntimeService runtimeService) {
    super(runtimeService);
  }

  @Override
  public void execute(ActivityExecutorContext<DemoteRoomOwner> execution) {
    DemoteRoomOwner demoteRoomOwner = execution.getActivity();

    if (this.isObo(demoteRoomOwner)) {
      this.doOboWithCache(execution);
    } else {
      for (Long uid : demoteRoomOwner.getUserIds()) {
        log.debug("Demote owner {} for room {}", uid, demoteRoomOwner.getStreamId());
        execution.bdk().streams().demoteUserToRoomParticipant(uid, demoteRoomOwner.getStreamId());
      }
    }
  }

  @Override
  protected Void doOboWithCache(ActivityExecutorContext<DemoteRoomOwner> execution) {
    DemoteRoomOwner demoteRoomOwner = execution.getActivity();
    AuthSession authSession = this.getOboAuthSession(execution);

    for (Long uid : demoteRoomOwner.getUserIds()) {
      log.debug("Demote owner {} for room {}", uid, demoteRoomOwner.getStreamId());
      execution.bdk().obo(authSession).streams().demoteUserToRoomParticipant(uid, demoteRoomOwner.getStreamId());
    }

    return null;
  }
}
