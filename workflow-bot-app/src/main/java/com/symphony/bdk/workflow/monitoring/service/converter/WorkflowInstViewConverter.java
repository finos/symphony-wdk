package com.symphony.bdk.workflow.monitoring.service.converter;

import com.symphony.bdk.workflow.api.v1.dto.StatusEnum;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstView;
import com.symphony.bdk.workflow.converter.Converter;
import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowInstanceDomain;

import org.springframework.stereotype.Component;

@Component
public class WorkflowInstViewConverter implements Converter<WorkflowInstanceDomain, WorkflowInstView> {

  @Override
  public WorkflowInstView apply(WorkflowInstanceDomain domain) {
    return WorkflowInstView.builder()
        .id(domain.getName())
        .version(domain.getVersion())
        .instanceId(domain.getInstanceId())
        .status(StatusEnum.toInstanceStatusEnum(domain.getStatus()))
        .startDate(domain.getStartDate())
        .endDate(domain.getEndDate())
        .build();
  }
}
