package com.symphony.bdk.workflow.versioning.repository;

import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;
import com.symphony.bdk.workflow.versioning.model.VersionedWorkflowId;

import org.springframework.data.repository.CrudRepository;

public interface VersionedWorkflowRepository extends CrudRepository<VersionedWorkflow, VersionedWorkflowId> {

}
