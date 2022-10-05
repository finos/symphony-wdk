package com.symphony.bdk.workflow.monitoring.repository;

import com.symphony.bdk.workflow.monitoring.repository.domain.VariablesDomain;

import java.time.Instant;
import java.util.List;

public interface VariableQueryRepository extends QueryRepository<VariablesDomain, String> {
  VariablesDomain findVarsByWorkflowInstanceIdAndVarName(String id, String name);

  List<VariablesDomain> findGlobalVarsHistoryByWorkflowInstId(String id, Instant occurredBefore, Instant occurredAfter);
}
