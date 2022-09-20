package com.symphony.bdk.workflow.api.v1;

import com.symphony.bdk.workflow.api.v1.dto.ErrorResponse;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowActivitiesView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowDefinitionView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowExecutionRequest;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;
import com.symphony.bdk.workflow.security.Authorized;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@Api("Api to execute and monitor workflows")
public interface WorkflowsApi {
  String X_MONITORING_TOKEN_KEY = "X-Monitoring-Token";

  @ApiOperation("Triggers the execution of a workflow given by its id. This is an asynchronous operation.")
  @ApiResponses(value = {@ApiResponse(code = 204, message = "", response = Object.class),
      @ApiResponse(code = 404, message = "No workflow found with id {id}", response = ErrorResponse.class),
      @ApiResponse(code = 401, message = "Request token is not valid", response = ErrorResponse.class)})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PostMapping("/{id}/execute")
  ResponseEntity<Object> executeWorkflowById(
      @ApiParam("Workflow's token to authenticate the request") @RequestHeader(name = "X-Workflow-Token") String token,
      @ApiParam("Workflow's id that is provided in SWADL") @PathVariable String id,
      @ApiParam("Arguments to be passed to the event triggering the workflow") @RequestBody
          WorkflowExecutionRequest arguments);

  @ApiOperation("List all deployed workflows")
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "OK", response = WorkflowView.class, responseContainer = "List"),
          @ApiResponse(code = 401, message = "Request token is not valid", response = ErrorResponse.class)})
  @Authorized(headerTokenKey = X_MONITORING_TOKEN_KEY)
  @GetMapping("/")
  ResponseEntity<List<WorkflowView>> listAllWorkflows(
      @ApiParam("Workflows monitoring token to authenticate the request") @RequestHeader(name = X_MONITORING_TOKEN_KEY)
          String token);

  @ApiOperation("List all instances of a given workflow")
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "OK", response = WorkflowInstView.class, responseContainer = "List"),
          @ApiResponse(code = 401, message = "Request token is not valid", response = ErrorResponse.class)})
  @Authorized(headerTokenKey = X_MONITORING_TOKEN_KEY)
  @GetMapping("/{workflowId}/instances")
  ResponseEntity<List<WorkflowInstView>> listWorkflowInstances(@PathVariable String workflowId,
      @ApiParam("Workflows monitoring token to authenticate the request") @RequestHeader(name = X_MONITORING_TOKEN_KEY)
          String token);

  @ApiOperation("List the completed activities in a given instance for a given workflow")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = WorkflowActivitiesView.class),
      @ApiResponse(code = 401, message = "Request token is not valid", response = ErrorResponse.class)})
  @Authorized(headerTokenKey = X_MONITORING_TOKEN_KEY)
  @GetMapping("/{workflowId}/instances/{instanceId}/activities")
  ResponseEntity<WorkflowActivitiesView> listInstanceActivities(@PathVariable String workflowId,
      @PathVariable String instanceId,
      @ApiParam("Workflows monitoring token to authenticate the request") @RequestHeader(name = X_MONITORING_TOKEN_KEY)
          String token);

  @ApiOperation("List activities definitions for a given workflow")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = WorkflowDefinitionView.class),
      @ApiResponse(code = 401, message = "Request token is not valid", response = ErrorResponse.class)})
  @Authorized(headerTokenKey = X_MONITORING_TOKEN_KEY)
  @GetMapping("/{workflowId}/definitions")
  ResponseEntity<WorkflowDefinitionView> listWorkflowActivities(@PathVariable String workflowId,
      @ApiParam("Workflows monitoring token to authenticate the request") @RequestHeader(name = X_MONITORING_TOKEN_KEY)
          String token);
}
