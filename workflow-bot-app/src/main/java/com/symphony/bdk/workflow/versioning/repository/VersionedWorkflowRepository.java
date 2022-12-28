package com.symphony.bdk.workflow.versioning.repository;

import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;
import com.symphony.bdk.workflow.versioning.model.VersionedWorkflowId;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VersionedWorkflowRepository extends JpaRepository<VersionedWorkflow, VersionedWorkflowId> {
  List<VersionedWorkflow> findByVersionedWorkflowIdId(String workflowId);

  Optional<VersionedWorkflow> findByPath(String path);
}
