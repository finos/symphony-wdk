package com.symphony.bdk.workflow.versioning.repository;

import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;
import com.symphony.bdk.workflow.versioning.model.VersionedWorkflowId;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VersionedWorkflowRepository extends JpaRepository<VersionedWorkflow, VersionedWorkflowId> {

}
