package com.symphony.bdk.workflow.jparepo;

import com.symphony.bdk.workflow.jpamodel.VersionedWorkflow;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface VersionedWorkflowRepository extends CrudRepository<VersionedWorkflow, String> {
  Optional<VersionedWorkflow> findById(String workflowId);
}
