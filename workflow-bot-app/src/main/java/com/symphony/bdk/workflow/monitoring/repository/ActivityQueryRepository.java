package com.symphony.bdk.workflow.monitoring.repository;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstLifeCycleFilter;
import com.symphony.bdk.workflow.monitoring.repository.domain.ActivityInstanceDomain;

import java.util.List;

public interface ActivityQueryRepository extends QueryRepository<ActivityInstanceDomain, String> {
  List<ActivityInstanceDomain> findAllByWorkflowInstanceId(String workflowId, String instanceId,
      WorkflowInstLifeCycleFilter lifeCycleFilter);
}
