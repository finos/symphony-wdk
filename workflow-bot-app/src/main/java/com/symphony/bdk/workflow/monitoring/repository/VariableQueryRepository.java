package com.symphony.bdk.workflow.monitoring.repository;

import com.symphony.bdk.workflow.monitoring.repository.domain.VariablesDomain;

public interface VariableQueryRepository extends QueryRepository<VariablesDomain, String> {
  VariablesDomain findGlobalByWorkflowInstanceId(String id);
}
