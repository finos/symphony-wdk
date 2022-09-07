package com.symphony.bdk.workflow.engine.camunda.monitor.repository;

import java.util.List;

public interface WorkflowMonitoringRepository<T> {
  List<T> listAllWorkflows();
}
