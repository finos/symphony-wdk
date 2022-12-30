package com.symphony.bdk.workflow.versioning.service;

import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;
import com.symphony.bdk.workflow.versioning.repository.VersionedWorkflowRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Component
@Transactional
public class VersioningService {

  private final VersionedWorkflowRepository versionedWorkflowRepository;

  public VersioningService(VersionedWorkflowRepository versionedWorkflowRepository) {
    this.versionedWorkflowRepository = versionedWorkflowRepository;
  }

  public void save(String workflowId, String version, String swadl, String path, boolean isToPublish) {
    VersionedWorkflow versionedWorkflow = new VersionedWorkflow()
        .setWorkflowId(workflowId)
        .setVersion(version)
        .setSwadl(swadl)
        .setPath(path)
        .setIsToPublish(isToPublish);
    this.versionedWorkflowRepository.save(versionedWorkflow);
  }

  public void save(String workflowId, String version, String swadl, String path) {
    save(workflowId, version, swadl, path, true);
  }

  public void delete(String workflowId, String version) {
    VersionedWorkflow workflowToDelete = new VersionedWorkflow()
        .setWorkflowId(workflowId)
        .setVersion(version);
    this.versionedWorkflowRepository.delete(workflowToDelete);
  }

  public Optional<VersionedWorkflow> findByWorkflowIdAndVersion(String workflowId, String version) {
    return this.versionedWorkflowRepository.findByWorkflowIdAndVersion(workflowId, version);
  }

  public List<VersionedWorkflow> findByWorkflowId(String workflowId) {
    return this.versionedWorkflowRepository.findByWorkflowId(workflowId);
  }

  public Optional<VersionedWorkflow> findByPath(Path path) {
    return this.versionedWorkflowRepository.findByPath(path.toString());
  }
}
