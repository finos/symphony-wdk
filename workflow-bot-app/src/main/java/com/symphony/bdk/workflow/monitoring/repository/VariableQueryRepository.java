package com.symphony.bdk.workflow.monitoring.repository;

import com.symphony.bdk.workflow.monitoring.repository.domain.VariablesDomain;

import java.util.List;

public interface VariableQueryRepository extends QueryRepository<VariablesDomain, String> {
  VariablesDomain findGlobalVarsByWorkflowInstanceId(String id);

  List<VariablesDomain> findGlobalVarsHistoryByWorkflowInstId(String id);
}
