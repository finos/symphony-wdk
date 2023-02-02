package com.symphony.bdk.workflow.monitoring.repository;

import com.symphony.bdk.workflow.api.v1.dto.StatusEnum;
import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowInstanceDomain;

import java.util.List;

public interface WorkflowInstQueryRepository extends QueryRepository<WorkflowInstanceDomain, String> {
  List<WorkflowInstanceDomain> findAllById(String id);

  List<WorkflowInstanceDomain> findAllByIdAndStatus(String id, StatusEnum status);

  List<WorkflowInstanceDomain> findAllByIdAndVersion(String id, String version);

  List<WorkflowInstanceDomain> findAllByIdAndStatusAndVersion(String id, StatusEnum status, String version);
}
