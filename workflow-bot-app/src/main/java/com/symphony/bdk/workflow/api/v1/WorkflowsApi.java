package com.symphony.bdk.workflow.api.v1;

import com.symphony.bdk.workflow.api.v1.dto.ErrorResponse;
import com.symphony.bdk.workflow.api.v1.dto.VariableView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowDefinitionView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowExecutionRequest;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowNodesView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.Instant;
import java.util.List;

@Api("Api to execute and monitor workflows")
public interface WorkflowsApi {
  String X_MONITORING_TOKEN_KEY = "X-Monitoring-Token";

  @ApiOperation("Triggers the execution of a workflow given by its id. This is an asynchronous operation.")
  @ApiResponses(value = {@ApiResponse(code = 204, message = "", response = Object.class),
      @ApiResponse(code = 404, message = "No workflow found with id {id}", response = ErrorResponse.class),
      @ApiResponse(code = 401, message = "Request is not authorised", response = ErrorResponse.class)})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PostMapping("/{id}/execute")
  ResponseEntity<Object> executeWorkflowById(
      @ApiParam(value = "Workflow's token to authenticate the request", required = true)
      @RequestHeader(name = "X-Workflow-Token") String token,
      @ApiParam(value = "Workflow's id that is provided in SWADL", required = true) @PathVariable String id,
      @ApiParam(value = "Arguments to be passed to the event triggering the workflow") @RequestBody
      WorkflowExecutionRequest arguments);

  @ApiOperation("List all deployed workflows")
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "OK", response = WorkflowView.class, responseContainer = "List"),
          @ApiResponse(code = 401, message = "Request is not authorised", response = ErrorResponse.class)})
  @GetMapping("/")
  ResponseEntity<List<WorkflowView>> listAllWorkflows(
      @ApiParam("Workflows monitoring token to authenticate the request")
      @RequestHeader(name = X_MONITORING_TOKEN_KEY) String token);

  @ApiOperation("List all instances of a given workflow")
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "OK", response = WorkflowInstView.class, responseContainer = "List"),
          @ApiResponse(code = 401, message = "Request is not authorised", response = ErrorResponse.class)})
  @GetMapping("/{workflowId}/instances")
  ResponseEntity<List<WorkflowInstView>> listWorkflowInstances(
      @ApiParam(value = "Workflow's id to list instances", required = true) @PathVariable String workflowId,
      @ApiParam(value = "Workflows monitoring token to authenticate the request")
      @RequestHeader(name = X_MONITORING_TOKEN_KEY) String token,
      @ApiParam("Optional query parameter to filter instances by status [Pending | Completed | Failed]")
      @RequestParam(required = false) String status,
      @ApiParam("Optional version parameter to filter instances by version")
      @RequestParam(required = false) Long version);

  @ApiOperation("List the completed activities in a given instance for a given workflow")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = WorkflowNodesView.class),
      @ApiResponse(code = 401, message = "Request is not authorised", response = ErrorResponse.class)})
  @GetMapping("/{workflowId}/instances/{instanceId}/states")
  ResponseEntity<WorkflowNodesView> getInstanceState(
      @ApiParam(value = "Workflow's id to list instance activities", required = true) @PathVariable String workflowId,
      @ApiParam(value = "Workflow's instance id to list activities", required = true) @PathVariable String instanceId,
      @ApiParam("Workflows monitoring token to authenticate the request")
      @RequestHeader(name = X_MONITORING_TOKEN_KEY) String token,
      @ApiParam(
          value = "Optional query parameter to filter activities having started before the date. "
              + "The date is an ISO 8601 date",
          name = "started_before", example = "2022-09-21T15:43:24.917Z")
      @RequestParam(required = false, name = "started_before") Instant startedBefore,
      @ApiParam(
          value = "Optional query parameter to filter activities having started after the date. "
              + "The date is an ISO 8601 date",
          name = "started_after", example = "2022-09-21T15:43:24.917Z")
      @RequestParam(required = false, name = "started_after") Instant startedAfter,
      @ApiParam(
          value = "Optional query parameter to filter activities having finished before the date. "
              + "The date is an ISO 8601 date",
          name = "finished_before", example = "2022-09-21T15:43:24.917Z")
      @RequestParam(required = false, name = "finished_before") Instant finishedBefore,
      @ApiParam(
          value = "Optional query parameter to filter activities having finished after the date. "
              + "The date is an ISO 8601 date",
          name = "finished_after", example = "2022-09-21T15:43:24.917Z")
      @RequestParam(required = false, name = "finished_after") Instant finishedAfter
  );

  @ApiOperation("Get activities definitions for a given workflow")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = WorkflowDefinitionView.class),
      @ApiResponse(code = 401, message = "Request is not authorised", response = ErrorResponse.class)})
  @GetMapping("/{workflowId}/definitions")
  ResponseEntity<WorkflowDefinitionView> getWorkflowDefinition(
      @ApiParam(value = "Workflow's id to get activities definitions", required = true) @PathVariable String workflowId,
      @ApiParam("Workflows monitoring token to authenticate the request")
      @RequestHeader(name = X_MONITORING_TOKEN_KEY) String token,
      @ApiParam("Optional version parameter to filter instances by version")
      @RequestParam(required = false) Long version);

  @ApiOperation("List global variables for a given workflow")
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "OK", response = VariableView.class, responseContainer = "List"),
          @ApiResponse(code = 401, message = "Request is not authorised", response = ErrorResponse.class)})
  @GetMapping("/{workflowId}/instances/{instanceId}/variables")
  ResponseEntity<List<VariableView>> listWorkflowGlobalVariables(
      @ApiParam(value = "Workflow's id to list global variables", required = true) @PathVariable String workflowId,
      @ApiParam(value = "Workflow's instance id to list global variables", required = true) @PathVariable
      String instanceId,
      @ApiParam("Workflows monitoring token to authenticate the request")
      @RequestHeader(name = X_MONITORING_TOKEN_KEY) String token,
      @ApiParam(value = "Optional query parameter to filter global variables update occurred before the date. "
          + "The date is an ISO 8601 date", example = "2022-09-21T15:43:24.917Z")
      @RequestParam(required = false, name = "updated_before") Instant updatedBefore,
      @ApiParam(value = "Optional query parameter to filter global variables update occurred after the date. "
          + "The date is an ISO 8601 date", example = "2022-09-21T15:43:24.917Z")
      @RequestParam(required = false, name = "updated_after") Instant updatedAfter);

}
