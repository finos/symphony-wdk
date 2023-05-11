package com.symphony.bdk.workflow.management.repository;

import com.symphony.bdk.workflow.configuration.ConditionalOnPropertyNotEmpty;
import com.symphony.bdk.workflow.management.repository.domain.WorkflowExpirationJob;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnPropertyNotEmpty("wdk.properties.management-token")
public interface WorkflowExpirationJobRepository extends JpaRepository<WorkflowExpirationJob, String> {
}
