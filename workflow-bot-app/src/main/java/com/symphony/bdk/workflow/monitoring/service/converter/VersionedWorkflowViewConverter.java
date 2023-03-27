package com.symphony.bdk.workflow.monitoring.service.converter;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;
import com.symphony.bdk.workflow.converter.Converter;
import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;

import org.springframework.stereotype.Component;

@Component
public class VersionedWorkflowViewConverter implements Converter<VersionedWorkflow, WorkflowView> {

  @Override
  public WorkflowView apply(VersionedWorkflow workflow) {
    return WorkflowView.builder()
        .id(workflow.getWorkflowId())
        .version(workflow.getVersion())
        .createdBy(workflow.getCreatedBy())
        .build();
  }
}
