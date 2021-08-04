package com.symphony.bdk.workflow.engine;

public interface WorkflowEngineMetrics {
  long countRunningProcesses();

  long countDeployedWorkflows();

  long countCompletedProcesses();

  long countRunningActivities();

  long countCompletedActivities();
}
