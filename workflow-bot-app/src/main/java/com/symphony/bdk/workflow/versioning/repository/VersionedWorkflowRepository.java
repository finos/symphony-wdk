package com.symphony.bdk.workflow.versioning.repository;

import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VersionedWorkflowRepository extends JpaRepository<VersionedWorkflow, Long> {
  Optional<VersionedWorkflow> findByWorkflowIdAndVersion(String workflowId, String version);

  List<VersionedWorkflow> findByWorkflowId(String workflowId);

  Optional<VersionedWorkflow> findByPath(String path);
}
