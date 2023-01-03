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

  public void save(String workflowId, String version, String swadl, String path, String deploymentId) {
    save(workflowId, version, swadl, path, deploymentId, true);
  }

  public void save(String workflowId, String version, String swadl, String path, String deploymentId,
      boolean isToPublish) {
    VersionedWorkflow versionedWorkflow = new VersionedWorkflow()
        .setWorkflowId(workflowId)
        .setVersion(version)
        .setDeploymentId(deploymentId)
        .setSwadl(swadl)
        .setPath(path)
        .setIsToPublish(isToPublish);
    this.save(versionedWorkflow, deploymentId);
  }

  public void save(VersionedWorkflow versionedWorkflow, String deploymentId) {
    versionedWorkflow.setDeploymentId(deploymentId);
    this.versionedWorkflowRepository.save(versionedWorkflow);
  }

  public void delete(String workflowId) {
    this.versionedWorkflowRepository.deleteAllByWorkflowId(workflowId);
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
