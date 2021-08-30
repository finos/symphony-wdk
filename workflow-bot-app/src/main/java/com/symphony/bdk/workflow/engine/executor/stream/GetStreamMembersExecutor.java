package com.symphony.bdk.workflow.engine.executor.stream;

import com.symphony.bdk.core.service.pagination.model.PaginationAttribute;
import com.symphony.bdk.gen.api.model.V2MembershipList;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.stream.GetStreamMembers;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetStreamMembersExecutor implements ActivityExecutor<GetStreamMembers> {

  private static final String OUTPUTS_MEMBERS_KEY = "members";

  @Override
  public void execute(ActivityExecutorContext<GetStreamMembers> execution) {
    GetStreamMembers getStreamMembers = execution.getActivity();
    String streamId = getStreamMembers.getStreamId();

    log.debug("Getting stream members for stream {}", streamId);
    V2MembershipList members;
    if (getStreamMembers.getLimitAsInt() != null && getStreamMembers.getSkipAsInt() != null) {
      members = execution.bdk().streams().listStreamMembers(streamId,
          new PaginationAttribute(getStreamMembers.getSkipAsInt(), getStreamMembers.getLimitAsInt()));
    } else if (getStreamMembers.getLimitAsInt() == null && getStreamMembers.getSkipAsInt() == null) {
      members = execution.bdk().streams().listStreamMembers(streamId);
    } else {
      throw new IllegalArgumentException("skip and limit should both be set to get stream members");
    }
    execution.setOutputVariable(OUTPUTS_MEMBERS_KEY, members);
  }

}
