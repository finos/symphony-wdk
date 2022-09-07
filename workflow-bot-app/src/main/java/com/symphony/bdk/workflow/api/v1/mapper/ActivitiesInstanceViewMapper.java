package com.symphony.bdk.workflow.api.v1.mapper;

import com.symphony.bdk.workflow.api.v1.dto.ActivitiesInstancesView;
import com.symphony.bdk.workflow.api.v1.dto.ActivityInstanceView;
import com.symphony.bdk.workflow.api.v1.dto.InstanceStatusEnum;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.history.HistoricActivityInstance;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ActivitiesInstanceViewMapper {

  private ActivitiesInstanceViewMapper() {}

  public static ActivitiesInstancesView toActivitiesView(List<HistoricActivityInstance> historicActivityInstances,
      String workflowId) {
    List<ActivityInstanceView> activityInstanceViewList = historicActivityInstances.stream()
        .map(ActivitiesInstanceViewMapper::toActivityView)
        .collect(Collectors.toList());

    return ActivitiesInstancesView.builder()
        .workflowId(workflowId)
        .activities(activityInstanceViewList)
        .build();
  }

  public static ActivityInstanceView toActivityView(HistoricActivityInstance historicActivityInstance) {
    ActivityInstanceView.ActivityInstanceViewBuilder builder = ActivityInstanceView.builder()
        .instanceId(historicActivityInstance.getProcessInstanceId());

    builder.activityId(historicActivityInstance.getActivityName());

    if (historicActivityInstance.getStartTime() != null) {
      builder.startDate(historicActivityInstance.getStartTime().getTime());
    }

    if (historicActivityInstance.getEndTime() != null) {
      builder.endDate(historicActivityInstance.getEndTime().getTime());
      builder.status(InstanceStatusEnum.COMPLETED);
    } else {
      builder.status(InstanceStatusEnum.PENDING);
    }
    //TODO: handle case when activity has not started yet: maybe not from historic repository but rather runtime repo
    //TODO: make sure it is not overridden here

    return builder.variablesModified(Collections.emptyList()).build();
    ///nextActivityIds(Collections.emptyList()).previousActivityId("")
  }
}
