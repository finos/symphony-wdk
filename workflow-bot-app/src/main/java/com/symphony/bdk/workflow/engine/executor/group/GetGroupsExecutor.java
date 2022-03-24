package com.symphony.bdk.workflow.engine.executor.group;

import com.symphony.bdk.ext.group.gen.api.model.GroupList;
import com.symphony.bdk.ext.group.gen.api.model.SortOrder;
import com.symphony.bdk.ext.group.gen.api.model.Status;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.group.GetGroups;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetGroupsExecutor implements ActivityExecutor<GetGroups> {

  private static final String OUTPUTS_GROUP_KEY = "groups";

  @Override
  public void execute(ActivityExecutorContext<GetGroups> execution) {
    log.debug("Getting groups");

    String typeId = execution.getActivity().getType();
    Status status = toStatus(execution.getActivity().getStatus());
    Integer limit = execution.getActivity().getLimit();
    SortOrder sort = toSortOrder(execution.getActivity().getSortOrder());
    String before = execution.getActivity().getBefore();
    String after = execution.getActivity().getAfter();

    GroupList groups = execution.bdk().groups().listGroups(typeId, status, before, after, limit, sort);

    execution.setOutputVariable(OUTPUTS_GROUP_KEY, groups);
  }

  private Status toStatus(String status) {
    if (status == null) {
      return null;
    } else {
      return Status.fromValue(status);
    }
  }

  private SortOrder toSortOrder(String sortOrder) {
    if (sortOrder == null) {
      return null;
    } else {
      return SortOrder.fromValue(sortOrder);
    }
  }

}
