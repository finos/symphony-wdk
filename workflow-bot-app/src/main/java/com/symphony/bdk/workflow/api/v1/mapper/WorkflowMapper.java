package com.symphony.bdk.workflow.api.v1.mapper;

import com.symphony.bdk.workflow.api.v1.dto.ActivitiesView;
import com.symphony.bdk.workflow.api.v1.dto.ActivityView;
import com.symphony.bdk.workflow.api.v1.dto.InstanceStatusEnum;
import com.symphony.bdk.workflow.api.v1.dto.InstanceView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class WorkflowMapper {

  private WorkflowMapper() {}

  public static WorkflowView toWorkflowView(ProcessDefinition processDefinition) {
    return WorkflowView.builder()
        .workflowId(processDefinition.getName())
        .version(processDefinition.getVersion())
        .build();
  }

  public static InstanceView toWorkflowInstanceView(HistoricProcessInstance historicProcessInstance) {
    InstanceStatusEnum status = toInstanceStatusEnum(historicProcessInstance.getState());

    InstanceView.InstanceViewBuilder builder = InstanceView.builder()
        .workflowId(historicProcessInstance.getProcessDefinitionKey())
        .instanceId(historicProcessInstance.getId())
        .status(status);

    if (historicProcessInstance.getStartTime() != null) {
      builder.startDate(historicProcessInstance.getStartTime().getTime());
    }

    if (historicProcessInstance.getEndTime() != null) {
      builder.endDate(historicProcessInstance.getEndTime().getTime());
    }

    return builder.build();
  }

  public static ActivitiesView toActivitiesView(List<HistoricActivityInstance> historicActivityInstances,
      String workflowId) {
    List<ActivityView> activityViewList = historicActivityInstances.stream()
        .map(a -> toActivityView(a, workflowId))
        .collect(Collectors.toList());

    return ActivitiesView.builder().activities(activityViewList).build();
  }

  public static ActivityView toActivityView(HistoricActivityInstance historicActivityInstance, String workflowId) {
    ActivityView.ActivityViewBuilder builder = ActivityView.builder()
        .workflowId(workflowId)
        .instanceId(historicActivityInstance.getProcessInstanceId())
        .activityId(historicActivityInstance.getActivityName())
        .type("type to define"); //TODO: pass the type in parameter, it can be brought from the mapping/workflow object

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

    return builder.nextActivityIds(Collections.emptyList())
        .previousActivityId("")
        .variablesModified(Collections.emptyList())
        .build();
  }

  private static InstanceStatusEnum toInstanceStatusEnum(String status) {
    return status.equals("ACTIVE") ? InstanceStatusEnum.PENDING : InstanceStatusEnum.COMPLETED;
  }
}
