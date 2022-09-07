package com.symphony.bdk.workflow.api.v1.mapper;

import com.symphony.bdk.workflow.api.v1.dto.InstanceStatusEnum;
import com.symphony.bdk.workflow.api.v1.dto.InstanceView;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.history.HistoricProcessInstance;

@Slf4j
public class WorkflowInstanceViewMapper {

  private WorkflowInstanceViewMapper() {}

  public static InstanceView toWorkflowInstanceView(HistoricProcessInstance historicProcessInstance) {
    InstanceStatusEnum status = InstanceStatusEnum.toInstanceStatusEnum(historicProcessInstance.getState());

    InstanceView.InstanceViewBuilder builder = InstanceView.builder()
        .workflowId(historicProcessInstance.getProcessDefinitionKey())
        .instanceId(historicProcessInstance.getId())
        .workflowVersion(historicProcessInstance.getProcessDefinitionVersion())
        .status(status);

    if (historicProcessInstance.getStartTime() != null) {
      builder.startDate(historicProcessInstance.getStartTime().getTime());
    }

    if (historicProcessInstance.getEndTime() != null) {
      builder.endDate(historicProcessInstance.getEndTime().getTime());
    }

    return builder.build();
  }
}
