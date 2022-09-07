package com.symphony.bdk.workflow.engine.camunda.monitor.repository;

import java.util.List;

public interface WorkflowInstanceMonitoringRepository<T> {
  List<T> listWorkflowInstances(String workflowId);
}
