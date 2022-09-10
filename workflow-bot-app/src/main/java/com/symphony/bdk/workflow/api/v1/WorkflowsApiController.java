package com.symphony.bdk.workflow.api.v1;

import com.symphony.bdk.workflow.api.v1.dto.ErrorResponse;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowActivitiesView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowDefinitionVIew;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowExecutionRequest;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;
import com.symphony.bdk.workflow.engine.ExecutionParameters;
import com.symphony.bdk.workflow.engine.UnauthorizedException;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.monitoring.service.MonitoringService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("v1/workflows")
@Slf4j
public class WorkflowsApiController {

  private final MonitoringService monitoringService;
  private final WorkflowEngine workflowEngine;

  public WorkflowsApiController(WorkflowEngine workflowEngine, MonitoringService monitoringService) {
    this.workflowEngine = workflowEngine;
    this.monitoringService = monitoringService;
  }

  /**
   * Triggers the execution of a workflow given by its id. This is an asynchronous operation.
   *
   * @param token     Workflow's token to authenticate the request
   * @param id        Workflow's id that is provided in SWADL
   * @param arguments Pass arguments to the event triggering the workflow
   */
  @PostMapping("/{id}/execute")
  public ResponseEntity<Object> executeWorkflowById(@RequestHeader(name = "X-Workflow-Token") String token,
      @PathVariable String id, @RequestBody WorkflowExecutionRequest arguments) {

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

  @GetMapping("/")
  public List<WorkflowView> listAllWorkflows() {
    return monitoringService.listAllWorkflows();
  }

  @GetMapping("/{workflowId}/instances")
  public List<WorkflowInstView> listWorkflowInstances(@PathVariable String workflowId) {
    return monitoringService.listWorkflowInstances(workflowId);
  }

  @GetMapping("/{workflowId}/instances/{instanceId}/activities")
  public WorkflowActivitiesView listInstanceActivities(@PathVariable String workflowId,
      @PathVariable String instanceId) {
    return monitoringService.listWorkflowInstanceActivities(workflowId, instanceId);
  }

  @GetMapping("/{workflowId}/definitions")
  public WorkflowDefinitionVIew listWorkflowActivities(@PathVariable String workflowId) {
    return monitoringService.listWorkflowActivities(workflowId);
  }

}
