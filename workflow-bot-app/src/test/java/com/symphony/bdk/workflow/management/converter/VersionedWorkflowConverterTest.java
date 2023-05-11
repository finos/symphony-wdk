package com.symphony.bdk.workflow.management.converter;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.symphony.bdk.workflow.api.v1.dto.VersionedWorkflowView;
import com.symphony.bdk.workflow.management.repository.domain.VersionedWorkflow;

import org.junit.jupiter.api.Test;

class VersionedWorkflowConverterTest {

  @Test
  void apply() {
    VersionedWorkflow versionedWorkflow = new VersionedWorkflow();
    versionedWorkflow.setVersion(1234L);
    versionedWorkflow.setWorkflowId("workflow");
    versionedWorkflow.setId("id");
    versionedWorkflow.setSwadl("swadl");
    versionedWorkflow.setPublished(true);
    versionedWorkflow.setActive(true);
    versionedWorkflow.setDescription("description");
    versionedWorkflow.setDeploymentId("deployment");
    versionedWorkflow.setCreatedBy(1234L);
    VersionedWorkflowConverter converter = new VersionedWorkflowConverter();
    VersionedWorkflowView workflowView = converter.apply(versionedWorkflow);
    assertThat(workflowView.getWorkflowId()).isEqualTo("workflow");
    assertThat(workflowView.getVersion()).isEqualTo(1234L);
    assertThat(workflowView.getId()).isEqualTo("id");
    assertThat(workflowView.getPublished()).isTrue();
    assertThat(workflowView.getActive()).isTrue();
    assertThat(workflowView.getDeploymentId()).isEqualTo("deployment");
    assertThat(workflowView.getDescription()).isEqualTo("description");
    assertThat(workflowView.getCreatedBy()).isEqualTo(1234L);
  }
}
