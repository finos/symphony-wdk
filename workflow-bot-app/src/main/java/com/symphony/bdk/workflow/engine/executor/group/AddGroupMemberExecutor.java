package com.symphony.bdk.workflow.engine.executor.group;

import com.symphony.bdk.ext.group.gen.api.model.ReadGroup;
import com.symphony.bdk.workflow.engine.executor.AbstractActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.group.AddGroupMember;
import com.symphony.bdk.workflow.swadl.v1.activity.group.CreateGroup;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddGroupMemberExecutor extends AbstractActivityExecutor<AddGroupMember>
    implements ActivityExecutor<AddGroupMember> {

  private static final String OUTPUTS_GROUP_KEY = "group";

  @Override
  public void execute(ActivityExecutorContext<AddGroupMember> execution) {
    String groupId = execution.getActivity().getGroupId();

    ReadGroup group = null;
    for (CreateGroup.GroupMember user : execution.getActivity().getMembers()) {
      log.debug("Add member {}/{} to group {}", user.getUserId(), user.getTenantId(), groupId);
      group = execution.bdk().groups().addMemberToGroup(groupId, user.getUserId());
    }

    if (group != null) {
      execution.setOutputVariable(OUTPUTS_GROUP_KEY, group);
    }
  }

}
