package com.symphony.bdk.workflow.versioning.service;

import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;
import com.symphony.bdk.workflow.versioning.repository.VersionedWorkflowRepository;

import org.springframework.stereotype.Component;

@Component
public class VersioningService {

  private final VersionedWorkflowRepository versionedWorkflowRepository;

  public VersioningService(VersionedWorkflowRepository versionedWorkflowRepository) {
    this.versionedWorkflowRepository = versionedWorkflowRepository;
  }

  public VersionedWorkflow save(String workflowId, String version, String swadl) {
    VersionedWorkflow versionedWorkflow = new VersionedWorkflow();
    versionedWorkflow.setWorkflowId(workflowId);
    versionedWorkflow.setSwadl(swadl);

    VersionedWorkflow entity = this.versionedWorkflowRepository.save(versionedWorkflow);

    System.out.println("debug");

    return entity;
  }
}
