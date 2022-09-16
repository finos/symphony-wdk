package com.symphony.bdk.workflow.api.v1;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowActivitiesView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowDefinitionView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowExecutionRequest;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;
import com.symphony.bdk.workflow.engine.ExecutionParameters;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.monitoring.service.MonitoringService;
import com.symphony.bdk.workflow.security.Authorized;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("v1/workflows")
@Slf4j
public class WorkflowsApiController implements WorkflowsApi {

  private final MonitoringService monitoringService;
  private final WorkflowEngine workflowEngine;

  public WorkflowsApiController(WorkflowEngine workflowEngine, MonitoringService monitoringService) {
    this.workflowEngine = workflowEngine;
    this.monitoringService = monitoringService;
  }

  @Override
  public ResponseEntity<Object> executeWorkflowById(String token, String id, WorkflowExecutionRequest arguments) {
    log.info("Executing workflow {}", id);
    workflowEngine.execute(id, new ExecutionParameters(arguments.getArgs(), token));

    return ResponseEntity.noContent().build();
  }

  @Override
  @Authorized(headerTokenKey = X_MONITORING_TOKEN_KEY)
  public ResponseEntity<List<WorkflowView>> listAllWorkflows(String token) {
    return ResponseEntity.ok(monitoringService.listAllWorkflows());
  }

  @Override
  @Authorized(headerTokenKey = X_MONITORING_TOKEN_KEY)
  public ResponseEntity<List<WorkflowInstView>> listWorkflowInstances(String workflowId, String token) {
    return ResponseEntity.ok(monitoringService.listWorkflowInstances(workflowId));
  }

  @Override
  @Authorized(headerTokenKey = X_MONITORING_TOKEN_KEY)
  public ResponseEntity<WorkflowActivitiesView> listInstanceActivities(String workflowId, String instanceId,
      String token) {
    return ResponseEntity.ok(monitoringService.listWorkflowInstanceActivities(workflowId, instanceId));
  }

  @Override
  @Authorized(headerTokenKey = X_MONITORING_TOKEN_KEY)
  public ResponseEntity<WorkflowDefinitionView> listWorkflowActivities(String workflowId, String token) {
    return ResponseEntity.ok(monitoringService.getWorkflowDefinition(workflowId));
  }

}
