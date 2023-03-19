package com.symphony.bdk.workflow.api.v1;

import com.symphony.bdk.workflow.api.v1.dto.ErrorResponse;
import com.symphony.bdk.workflow.api.v1.dto.SwadlView;
import com.symphony.bdk.workflow.api.v1.dto.VersionedWorkflowView;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

@Api("Api to manage workflow swadl lifecycle")
public interface WorkflowsMgtApi {
  String X_MANAGEMENT_TOKEN_KEY = "X-Management-Token";

  @ApiOperation("Validate and save/deploy a new workflow SWADL content.")
  @ApiResponses(value = {@ApiResponse(code = 204, message = ""),
      @ApiResponse(code = 400, message = "Invalid workflow swadl", response = ErrorResponse.class),
      @ApiResponse(code = 401, message = "Request is not authorised", response = ErrorResponse.class)})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<Void> saveAndDeploySwadl(
      @ApiParam(value = "Workflow's token to authenticate the request", required = true)
      @RequestHeader(name = X_MANAGEMENT_TOKEN_KEY) String token,
      @ApiParam(value = "Workflow SWADL form to create") @ModelAttribute SwadlView swadlView,
      HttpServletRequest request);

  @ApiOperation("Validate and update the latest workflow SWADL content.")
  @ApiResponses(value = {@ApiResponse(code = 204, message = ""),
      @ApiResponse(code = 400, message = "Invalid workflow swadl", response = ErrorResponse.class),
      @ApiResponse(code = 404, message = "No workflow found with id {id}", response = ErrorResponse.class),
      @ApiResponse(code = 401, message = "Request is not authorised", response = ErrorResponse.class)})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<Void> updateSwadl(
      @ApiParam(value = "Workflow's token to authenticate the request", required = true)
      @RequestHeader(name = X_MANAGEMENT_TOKEN_KEY) String token,
      @ApiParam(value = "Workflow SWADL form to update") @ModelAttribute SwadlView swadlView);

  @ApiOperation("Get all versioned workflows by the given ID.")
  @ApiResponses(value = {@ApiResponse(code = 204, message = ""),
      @ApiResponse(code = 404, message = "No workflow found with id {id}", response = ErrorResponse.class),
      @ApiResponse(code = 401, message = "Request is not authorised", response = ErrorResponse.class)})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @GetMapping("/{id}")
  ResponseEntity<List<VersionedWorkflowView>> getSwadl(
      @ApiParam(value = "Workflow's token to authenticate the request", required = true)
      @RequestHeader(name = X_MANAGEMENT_TOKEN_KEY) String token,
      @ApiParam(value = "Workflow's id that is provided in SWADL", required = true) @PathVariable String id);

  @ApiOperation("Delete the workflow by the given ID.")
  @ApiResponses(value = {@ApiResponse(code = 204, message = ""),
      @ApiResponse(code = 404, message = "No workflow found with id {id}", response = ErrorResponse.class),
      @ApiResponse(code = 401, message = "Request is not authorised", response = ErrorResponse.class)})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/{id}")
  ResponseEntity<Void> deleteSwadl(
      @ApiParam(value = "Workflow's token to authenticate the request", required = true)
      @RequestHeader(name = X_MANAGEMENT_TOKEN_KEY) String token,
      @ApiParam(value = "Workflow's id that is provided in SWADL", required = true) @PathVariable String id);

  @ApiOperation("Roll back a workflow version.")
  @ApiResponses(value = {@ApiResponse(code = 204, message = ""),
      @ApiResponse(code = 404, message = "No workflow found with id {id} and version {version}",
          response = ErrorResponse.class),
      @ApiResponse(code = 401, message = "Request is not authorised", response = ErrorResponse.class)})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PostMapping(value = "/{id}/versions/{version}")
  ResponseEntity<Void> deployActiveVersion(
      @ApiParam(value = "Workflow's token to authenticate the request", required = true)
      @RequestHeader(name = X_MANAGEMENT_TOKEN_KEY) String token,
      @ApiParam(value = "Workflow's id that is provided in SWADL", required = true) @PathVariable String id,
      @ApiParam(value = "Workflow's version to roll back to", required = true) @PathVariable Long version);

  @ApiOperation("Get a specific version workflow.")
  @ApiResponses(value = {@ApiResponse(code = 204, message = ""),
      @ApiResponse(code = 404, message = "No workflow found with id {id} and version {version}",
          response = ErrorResponse.class),
      @ApiResponse(code = 401, message = "Request is not authorised", response = ErrorResponse.class)})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @GetMapping(value = "/{id}/versions/{version}")
  ResponseEntity<VersionedWorkflowView> getSwadlByVersion(
      @ApiParam(value = "Workflow's token to authenticate the request", required = true)
      @RequestHeader(name = X_MANAGEMENT_TOKEN_KEY) String token,
      @ApiParam(value = "Workflow's id that is provided in SWADL", required = true) @PathVariable String id,
      @ApiParam(value = "Workflow's version to roll back to", required = true) @PathVariable Long version);

  @ApiOperation("Delete the workflow by the given ID and version.")
  @ApiResponses(value = {@ApiResponse(code = 204, message = ""),
      @ApiResponse(code = 404, message = "No workflow found with id {id}", response = ErrorResponse.class),
      @ApiResponse(code = 401, message = "Request is not authorised", response = ErrorResponse.class)})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/{id}/versions/{version}")
  ResponseEntity<Void> deleteSwadlByVersion(
      @ApiParam(value = "Workflow's token to authenticate the request", required = true)
      @RequestHeader(name = X_MANAGEMENT_TOKEN_KEY) String token,
      @ApiParam(value = "Workflow's id that is provided in SWADL", required = true) @PathVariable String id,
      @ApiParam(value = "Workflow version") @PathVariable Long version);

  @ApiOperation("Streaming logs in SSE.")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "", response = ResponseBodyEmitter.class),
      @ApiResponse(code = 401, message = "Request is not authorised", response = ErrorResponse.class)})
  @ResponseStatus(HttpStatus.OK)
  @GetMapping(path = "/logs", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  SseEmitter streamingLogs(
      @ApiParam(value = "Workflow's token to authenticate the request", required = true)
      @RequestHeader(name = X_MANAGEMENT_TOKEN_KEY) String token);

  @ApiOperation("Schedule a workflow expiration.")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
      @ApiResponse(code = 404, message = "No workflow found with id {workflowId}"),
      @ApiResponse(code = 401, message = "Request is not authorised", response = ErrorResponse.class)})
  @PostMapping(value = "/{workflowId}")
  ResponseEntity<Void> scheduleWorkflowExpirationJob(
      @ApiParam(value = "Workflow's id to expire.", required = true) @PathVariable String workflowId,
      @ApiParam(value = "Expiration date. Instant epoch.") @RequestBody Instant expirationDate);
}
