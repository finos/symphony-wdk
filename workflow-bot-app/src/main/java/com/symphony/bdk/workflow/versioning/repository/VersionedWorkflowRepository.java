package com.symphony.bdk.workflow.versioning.repository;

import com.symphony.bdk.workflow.configuration.ConditionalOnPropertyNotEmpty;
import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnPropertyNotEmpty("wdk.properties.management-token")
public interface VersionedWorkflowRepository extends JpaRepository<VersionedWorkflow, String> {
  List<VersionedWorkflow> findByWorkflowId(String workflowId);

  Optional<VersionedWorkflow> findByWorkflowIdAndVersion(String workflowId, Long version);

  Optional<VersionedWorkflow> findByWorkflowIdAndActiveTrue(String workflowId);

  Optional<VersionedWorkflow> findByWorkflowIdAndPublishedFalse(String workflowId);

  void deleteByWorkflowId(String workflowId);

  void deleteByWorkflowIdAndVersion(String id, Long version);
}
