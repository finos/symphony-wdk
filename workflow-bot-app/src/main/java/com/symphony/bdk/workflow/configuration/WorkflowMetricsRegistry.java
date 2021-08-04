package com.symphony.bdk.workflow.configuration;

import com.symphony.bdk.workflow.engine.WorkflowEngineMetrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

import java.util.function.ToDoubleFunction;

/**
 * Expose custom metrics (workflow.*) to Spring Boot Actuator.
 */
@Component
public class WorkflowMetricsRegistry {

  public WorkflowMetricsRegistry(MeterRegistry registry, WorkflowEngineMetrics metrics) {
    registry.gauge("workflow.deployed", Tags.empty(),
        (ToDoubleFunction<Tags>) value -> (double) metrics.countDeployedWorkflows());

    registry.gauge("workflow.process.running", Tags.empty(),
        (ToDoubleFunction<Tags>) value -> (double) metrics.countRunningProcesses());
    registry.gauge("workflow.process.completed", Tags.empty(),
        (ToDoubleFunction<Tags>) value -> (double) metrics.countCompletedProcesses());

    registry.gauge("workflow.activity.running", Tags.empty(),
        (ToDoubleFunction<Tags>) value -> (double) metrics.countRunningActivities());
    registry.gauge("workflow.activity.completed", Tags.empty(),
        (ToDoubleFunction<Tags>) value -> (double) metrics.countCompletedActivities());

    // TODO expose failed process/activity counters
  }

}
