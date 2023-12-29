package com.symphony.bdk.workflow.api.v1;

import com.symphony.bdk.workflow.api.v1.dto.VariableView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowExecutionRequest;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowNodesStateView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowNodesView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Api to execute and monitor workflows")
public interface WorkflowsApi {
  String X_MONITORING_TOKEN_KEY = "X-Monitoring-Token";

  @Operation(description = "Triggers the execution of a workflow given by its id. This is an asynchronous operation.")
  @ApiResponses(value = {@ApiResponse(responseCode = "204"),
      @ApiResponse(responseCode = "404", description = "No workflow found with id {id}"),
      @ApiResponse(responseCode = "401", description = "Request is not authorised")})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PostMapping("/{id}/execute")
  ResponseEntity<Object> executeWorkflowById(
      @Parameter(description = "Workflow's token to authenticate the request", required = true)
      @RequestHeader(name = "X-Workflow-Token") String token,
      @Parameter(description = "Workflow's id that is provided in SWADL", required = true) @PathVariable String id,
      @Parameter(description = "Arguments to be passed to the event triggering the workflow") @RequestBody
      WorkflowExecutionRequest arguments);

  @Operation(description = "List all deployed workflows")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "OK"),
          @ApiResponse(responseCode = "401", description = "Request is not authorised")})
  @GetMapping
  ResponseEntity<List<WorkflowView>> listAllWorkflows(
      @Parameter(description = "Workflows monitoring token to authenticate the request")
      @RequestHeader(name = X_MONITORING_TOKEN_KEY) String token);

  @Operation(description = "List all instances of a given workflow")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "OK"),
          @ApiResponse(responseCode = "401", description = "Request is not authorised")})
  @GetMapping("/{workflowId}/instances")
  ResponseEntity<List<WorkflowInstView>> listWorkflowInstances(
      @Parameter(description = "Workflow's id to list instances", required = true) @PathVariable String workflowId,
      @Parameter(description = "Workflows monitoring token to authenticate the request")
      @RequestHeader(name = X_MONITORING_TOKEN_KEY) String token,
      @Parameter(description = "Optional query parameter to filter instances by status [Pending | Completed | Failed]")
      @RequestParam(required = false) String status,
      @Parameter(description = "Optional version parameter to filter instances by version")
      @RequestParam(required = false) Long version);

  @Operation(description = "List the completed activities in a given instance for a given workflow")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "401", description = "Request is not authorised")})
  @GetMapping("/{workflowId}/instances/{instanceId}/states")
  ResponseEntity<WorkflowNodesStateView> getInstanceState(
      @Parameter(description = "Workflow's id to list instance activities", required = true) @PathVariable String workflowId,
      @Parameter(description = "Workflow's instance id to list activities", required = true) @PathVariable String instanceId,
      @Parameter(description = "Workflows monitoring token to authenticate the request")
      @RequestHeader(name = X_MONITORING_TOKEN_KEY) String token,
      @Parameter(
          description = "Optional query parameter to filter activities having started before the date. "
              + "The date is an ISO 8601 date",
          name = "started_before", example = "2022-09-21T15:43:24.917Z")
      @RequestParam(required = false, name = "started_before") Instant startedBefore,
      @Parameter(
          description = "Optional query parameter to filter activities having started after the date. "
              + "The date is an ISO 8601 date",
          name = "started_after", example = "2022-09-21T15:43:24.917Z")
      @RequestParam(required = false, name = "started_after") Instant startedAfter,
      @Parameter(
          description = "Optional query parameter to filter activities having finished before the date. "
              + "The date is an ISO 8601 date",
          name = "finished_before", example = "2022-09-21T15:43:24.917Z")
      @RequestParam(required = false, name = "finished_before") Instant finishedBefore,
      @Parameter(
          description = "Optional query parameter to filter activities having finished after the date. "
              + "The date is an ISO 8601 date",
          name = "finished_after", example = "2022-09-21T15:43:24.917Z")
      @RequestParam(required = false, name = "finished_after") Instant finishedAfter
  );

  @Operation(description = "Get activities graph nodes for a given workflow")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "401", description = "Request is not authorised")})
  @GetMapping("/{workflowId}/nodes")
  ResponseEntity<WorkflowNodesView> getWorkflowGraphNodes(
      @Parameter(description = "Workflow's id to get activities definitions", required = true) @PathVariable String workflowId,
      @Parameter(description = "Workflows monitoring token to authenticate the request")
      @RequestHeader(name = X_MONITORING_TOKEN_KEY) String token,
      @Parameter(description = "Optional version parameter to filter instances by version")
      @RequestParam(required = false) Long version);

  @Operation(description = "List global variables for a given workflow")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "OK"),
          @ApiResponse(responseCode = "401", description = "Request is not authorised")})
  @GetMapping("/{workflowId}/instances/{instanceId}/variables")
  ResponseEntity<List<VariableView>> listWorkflowGlobalVariables(
      @Parameter(description = "Workflow's id to list global variables", required = true) @PathVariable String workflowId,
      @Parameter(description = "Workflow's instance id to list global variables", required = true) @PathVariable
      String instanceId,
      @Parameter(description = "Workflows monitoring token to authenticate the request")
      @RequestHeader(name = X_MONITORING_TOKEN_KEY) String token,
      @Parameter(description = "Optional query parameter to filter global variables update occurred before the date. "
          + "The date is an ISO 8601 date", example = "2022-09-21T15:43:24.917Z")
      @RequestParam(required = false, name = "updated_before") Instant updatedBefore,
      @Parameter(description = "Optional query parameter to filter global variables update occurred after the date. "
          + "The date is an ISO 8601 date", example = "2022-09-21T15:43:24.917Z")
      @RequestParam(required = false, name = "updated_after") Instant updatedAfter);
}
