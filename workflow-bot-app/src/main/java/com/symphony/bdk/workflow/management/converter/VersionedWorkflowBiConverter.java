package com.symphony.bdk.workflow.management.converter;

import com.symphony.bdk.workflow.api.v1.dto.SwadlView;
import com.symphony.bdk.workflow.converter.BiConverter;
import com.symphony.bdk.workflow.management.repository.domain.VersionedWorkflow;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class VersionedWorkflowBiConverter implements BiConverter<Workflow, SwadlView, VersionedWorkflow> {
  private final String deploymentId;

  @Override
  public VersionedWorkflow apply(Workflow workflow, SwadlView swadlView) {
    Optional<String> optionalDeployId = Optional.ofNullable(deploymentId);
    VersionedWorkflow versionedWorkflow = new VersionedWorkflow();
    versionedWorkflow.setWorkflowId(workflow.getId());
    versionedWorkflow.setVersion(workflow.getVersion());
    versionedWorkflow.setDeploymentId(optionalDeployId.orElse(null));
    versionedWorkflow.setSwadl(swadlView.getSwadl());
    versionedWorkflow.setDescription(swadlView.getDescription());
    versionedWorkflow.setCreatedBy(swadlView.getCreatedBy());
    versionedWorkflow.setPublished(workflow.isToPublish());
    versionedWorkflow.setActive(optionalDeployId.isPresent() ? true : null);
    versionedWorkflow.setDeploymentId(optionalDeployId.orElse(null));
    return versionedWorkflow;
  }
}
