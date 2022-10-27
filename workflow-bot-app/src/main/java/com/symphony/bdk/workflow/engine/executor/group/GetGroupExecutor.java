package com.symphony.bdk.workflow.engine.executor.group;

import com.symphony.bdk.ext.group.gen.api.model.ReadGroup;
import com.symphony.bdk.workflow.engine.executor.AbstractActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.group.GetGroup;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetGroupExecutor extends AbstractActivityExecutor<GetGroup> implements ActivityExecutor<GetGroup> {

  private static final String OUTPUTS_GROUP_KEY = "group";

  @Override
  public void execute(ActivityExecutorContext<GetGroup> execution) {
    String groupId = execution.getActivity().getGroupId();
    log.debug("Getting group {}", groupId);
    ReadGroup group = execution.bdk().groups().getGroup(groupId);
    execution.setOutputVariable(OUTPUTS_GROUP_KEY, group);
  }

}
