package com.symphony.bdk.workflow.management.converter;

import com.symphony.bdk.workflow.api.v1.dto.VersionedWorkflowView;
import com.symphony.bdk.workflow.converter.Converter;
import com.symphony.bdk.workflow.management.repository.domain.VersionedWorkflow;

import org.springframework.stereotype.Component;

@Component
public class VersionedWorkflowConverter implements Converter<VersionedWorkflow, VersionedWorkflowView> {

  @Override
  public VersionedWorkflowView apply(VersionedWorkflow versionedWorkflow) {
    return VersionedWorkflowView.builder()
        .id(versionedWorkflow.getId())
        .workflowId(versionedWorkflow.getWorkflowId())
        .version(versionedWorkflow.getVersion())
        .active(versionedWorkflow.getActive())
        .published(versionedWorkflow.getPublished())
        .deploymentId(versionedWorkflow.getDeploymentId())
        .swadl(versionedWorkflow.getSwadl())
        .description(versionedWorkflow.getDescription())
        .createdBy(versionedWorkflow.getCreatedBy())
        .build();
  }
}
