package com.symphony.bdk.workflow.monitoring.repository;

import com.symphony.bdk.workflow.monitoring.repository.domain.ActivityInstanceDomain;

import java.util.List;

public interface ActivityQueryRepository extends QueryRepository<ActivityInstanceDomain, String> {
  List<ActivityInstanceDomain> findAllByWorkflowInstanceId(String instanceId);
}
