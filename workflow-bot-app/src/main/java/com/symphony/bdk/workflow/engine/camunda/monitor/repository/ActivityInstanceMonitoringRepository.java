package com.symphony.bdk.workflow.engine.camunda.monitor.repository;

import java.util.List;

public interface ActivityInstanceMonitoringRepository<T> {
  List<T> listInstanceActivities(String instanceId);
}
