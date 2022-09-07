package com.symphony.bdk.workflow.api.v1;

import com.symphony.bdk.workflow.api.v1.dto.ActivitiesInstancesView;
import com.symphony.bdk.workflow.api.v1.dto.InstanceView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowDefinitionVIew;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;
import com.symphony.bdk.workflow.engine.camunda.monitor.service.MonitoringService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("v1/monitoring/workflows")
@Slf4j
public class MonitoringApiController {

  private final MonitoringService monitoringService;

  public MonitoringApiController(MonitoringService monitoringService) {
    this.monitoringService = monitoringService;
  }

  @GetMapping("/")
  public List<WorkflowView> listAllWorkflows() {
    return monitoringService.listAllWorkflows();
  }

  @GetMapping("/{workflowId}/instances")
  public List<InstanceView> listWorkflowInstances(@PathVariable String workflowId) {
    return monitoringService.listWorkflowInstances(workflowId);
  }

  @GetMapping("/{workflowId}/instances/{instanceId}/activities")
  public ActivitiesInstancesView listInstanceActivities(@PathVariable String workflowId, @PathVariable String instanceId) {
    return monitoringService.listInstanceActivities(workflowId, instanceId);
  }

  @GetMapping("/{workflowId}/definitions")
  public WorkflowDefinitionVIew listWorkflowActivities(@PathVariable String workflowId) {
    return monitoringService.listWorkflowActivities(workflowId);
  }
}
