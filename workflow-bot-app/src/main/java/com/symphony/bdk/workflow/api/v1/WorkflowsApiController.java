package com.symphony.bdk.workflow.api.v1;

import com.symphony.bdk.workflow.WorkflowsApi;
import com.symphony.bdk.workflow.api.v1.dto.ErrorResponse;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowActivitiesView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowDefinitionView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowExecutionRequest;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;
import com.symphony.bdk.workflow.engine.ExecutionParameters;
import com.symphony.bdk.workflow.engine.UnauthorizedException;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.monitoring.service.Authorized;
import com.symphony.bdk.workflow.monitoring.service.MonitoringService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

  public ResponseEntity<Object> executeWorkflowById(String token, String id, WorkflowExecutionRequest arguments) {
    try {
      log.info("Executing workflow {}", id);
      workflowEngine.execute(id, new ExecutionParameters(arguments.getArgs(), token));

    } catch (IllegalArgumentException illegalArgumentException) {
      log.warn("The workflow id {} provided in the request does not exist", id);
      return new ResponseEntity<>(new ErrorResponse(illegalArgumentException.getMessage()), HttpStatus.NOT_FOUND);

    } catch (UnauthorizedException unauthorizedException) {
      log.warn("The token provided in the request is not valid for this workflow");
      return new ResponseEntity<>(new ErrorResponse(unauthorizedException.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    return ResponseEntity.noContent().build();
  }

  @Authorized(headerTokenKey = X_MONITORING_TOKEN_KEY)
  public List<WorkflowView> listAllWorkflows(String token) {
    return monitoringService.listAllWorkflows();
  }

  @Authorized(headerTokenKey = X_MONITORING_TOKEN_KEY)
  public List<WorkflowInstView> listWorkflowInstances(String workflowId, String token) {
    return monitoringService.listWorkflowInstances(workflowId);
  }

  @Authorized(headerTokenKey = X_MONITORING_TOKEN_KEY)
  public WorkflowActivitiesView listInstanceActivities(String workflowId, String instanceId, String token) {
    return monitoringService.listWorkflowInstanceActivities(workflowId, instanceId);
  }

  @Authorized(headerTokenKey = X_MONITORING_TOKEN_KEY)
  public WorkflowDefinitionView listWorkflowActivities(String workflowId, String token) {
    return monitoringService.listWorkflowActivities(workflowId);
  }

}
