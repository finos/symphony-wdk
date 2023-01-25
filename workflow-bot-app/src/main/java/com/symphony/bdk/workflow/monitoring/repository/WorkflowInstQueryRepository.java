package com.symphony.bdk.workflow.monitoring.repository;

import com.symphony.bdk.workflow.api.v1.dto.InstanceStatusEnum;
import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowInstanceDomain;

import java.util.List;

public interface WorkflowInstQueryRepository extends QueryRepository<WorkflowInstanceDomain, String> {
  List<WorkflowInstanceDomain> findAllById(String id);

  List<WorkflowInstanceDomain> findAllById(String id, InstanceStatusEnum status);
}
