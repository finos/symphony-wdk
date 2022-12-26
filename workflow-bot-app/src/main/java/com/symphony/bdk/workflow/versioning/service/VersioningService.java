package com.symphony.bdk.workflow.versioning.service;

import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;
import com.symphony.bdk.workflow.versioning.repository.VersionedWorkflowRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
public class VersioningService {

  private final VersionedWorkflowRepository versionedWorkflowRepository;

  public VersioningService(VersionedWorkflowRepository versionedWorkflowRepository) {
    this.versionedWorkflowRepository = versionedWorkflowRepository;
  }

  public void save(String workflowId, String version, String swadl) {
    VersionedWorkflow versionedWorkflow =
        new VersionedWorkflow().setVersionedWorkflowId(workflowId, version).setSwadl(swadl);
    this.versionedWorkflowRepository.save(versionedWorkflow);
    Iterable<VersionedWorkflow> all = this.versionedWorkflowRepository.findAll();
    System.out.println(all);
  }

  public List<VersionedWorkflow> findAll() {
    return this.versionedWorkflowRepository.findAll();
  }
}
