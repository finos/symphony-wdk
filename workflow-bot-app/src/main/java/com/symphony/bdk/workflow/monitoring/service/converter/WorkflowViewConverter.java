package com.symphony.bdk.workflow.monitoring.service.converter;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;
import com.symphony.bdk.workflow.converter.Converter;
import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowDomain;

import org.springframework.stereotype.Component;

@Component
public class WorkflowViewConverter implements Converter<WorkflowDomain, WorkflowView> {

  @Override
  public WorkflowView apply(WorkflowDomain workflowDomain) {
    return WorkflowView.builder()
        .id(workflowDomain.getName())
        .version(workflowDomain.getVersion())
        .build();
  }
}
