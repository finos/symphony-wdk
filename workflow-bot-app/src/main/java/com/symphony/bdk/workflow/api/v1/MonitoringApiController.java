package com.symphony.bdk.workflow.api.v1;

import com.symphony.bdk.workflow.api.v1.dto.ActivitiesView;
import com.symphony.bdk.workflow.api.v1.dto.InstanceView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;
import com.symphony.bdk.workflow.api.v1.mapper.WorkflowMapper;
import com.symphony.bdk.workflow.engine.camunda.monitor.MonitoringService;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

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

    return monitoringService.listAllWorkflows().stream()
        .map(WorkflowMapper::toWorkflowView)
        .collect(Collectors.toList());
  }

  @GetMapping("/{workflowId}/instances")
  public List<InstanceView> listWorkflowInstances(@PathVariable String workflowId) {
    return monitoringService.listWorkflowInstances(workflowId)
        .stream()
        .map(WorkflowMapper::toWorkflowInstanceView)
        .collect(Collectors.toList());
  }

  @GetMapping("/{workflowId}/instances/{instanceId}/activities")
  public ActivitiesView listInstanceActivities(@PathVariable String workflowId, @PathVariable String instanceId) {
    List<HistoricActivityInstance> activities = monitoringService.listInstanceActivities(instanceId);
    return WorkflowMapper.toActivitiesView(activities, workflowId);
  }
}
