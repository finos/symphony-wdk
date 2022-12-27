package com.symphony.bdk.workflow.versioning.service;

import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;
import com.symphony.bdk.workflow.versioning.model.VersionedWorkflowId;
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
        .setVersionedWorkflowId(workflowId, version)
        .setSwadl(swadl)
        .setPath(path)
        .setIsToPublish(isToPublish);
    this.versionedWorkflowRepository.save(versionedWorkflow);
  }

  public void save(String workflowId, String version, String swadl, String path) {
    save(workflowId, version, swadl, path, true); //isToPublish is true by default
  }

  public void delete(String workflowId, String version) {
    VersionedWorkflow workflowToDelete = new VersionedWorkflow()
        .setVersionedWorkflowId(workflowId, version);
    this.versionedWorkflowRepository.delete(workflowToDelete);
  }

  public Optional<VersionedWorkflow> find(String workflowId, String version) {
    return this.versionedWorkflowRepository.findById(new VersionedWorkflowId().id(workflowId).version(version));
  }

  public List<VersionedWorkflow> find(String workflowId) {
    return this.versionedWorkflowRepository.findByVersionedWorkflowIdId(workflowId);
  }

  public Optional<VersionedWorkflow> find(Path path) {
    return this.versionedWorkflowRepository.findByPath(path.toString());
  }
}
