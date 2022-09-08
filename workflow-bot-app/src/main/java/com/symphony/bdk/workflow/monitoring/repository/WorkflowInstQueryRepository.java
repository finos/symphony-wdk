package com.symphony.bdk.workflow.monitoring.repository;

import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowInstanceDomain;

import java.util.List;

public interface WorkflowInstQueryRepository extends QueryRepository<WorkflowInstanceDomain, String> {
  List<WorkflowInstanceDomain> findAllById(String id);
}
