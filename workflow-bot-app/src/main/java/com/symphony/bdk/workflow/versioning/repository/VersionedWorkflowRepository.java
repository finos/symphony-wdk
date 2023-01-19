package com.symphony.bdk.workflow.versioning.repository;

import com.symphony.bdk.workflow.configuration.ConditionalOnPropertyNotEmpty;
import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnPropertyNotEmpty("wdk.properties.management-token")
public interface VersionedWorkflowRepository extends JpaRepository<VersionedWorkflow, String> {
  List<VersionedWorkflow> findByWorkflowId(String workflowId);

  Optional<VersionedWorkflow> findByWorkflowIdAndVersion(String workflowId, Long version);

  Optional<VersionedWorkflow> findByWorkflowIdAndActiveTrue(String workflowId);

  Optional<VersionedWorkflow> findFirstByWorkflowIdOrderByVersionDesc(String workflowId);

  @Modifying
  @Query("DELETE FROM VersionedWorkflow f WHERE f.workflowId=:workflowId")
  void deleteByWorkflowId(@Param("workflowId") String workflowId);

  boolean existsByWorkflowId(String workflowId);
}
