package com.symphony.bdk.workflow.api.v1;

import com.symphony.bdk.workflow.api.v1.dto.ErrorResponse;
import com.symphony.bdk.workflow.api.v1.dto.SecretView;
import com.symphony.bdk.workflow.api.v1.dto.SwadlView;
import com.symphony.bdk.workflow.api.v1.dto.VersionedWorkflowView;
import com.symphony.bdk.workflow.engine.executor.SecretKeeper;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import javax.validation.Valid;

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
      @ApiParam(value = "Workflow SWADL form to create") @Valid @ModelAttribute SwadlView swadlView);

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
      @ApiParam(value = "Workflow SWADL form to update") @Valid @ModelAttribute SwadlView swadlView);

  @ApiOperation(
      "Get the active version of the workflow by the given ID, which is default behavior; if the version parameter"
          + "is given, that version will be returned; if all_versions flag is true, all versions will be returned; if"
          + "version is provided and versions flag is true, all versions will be returned.")
  @ApiResponses(value = {@ApiResponse(code = 204, message = ""),
      @ApiResponse(code = 404, message = "No workflow found with id {id}", response = ErrorResponse.class),
      @ApiResponse(code = 401, message = "Request is not authorised", response = ErrorResponse.class)})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @GetMapping(path = "/{workflowId}", produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<List<VersionedWorkflowView>> getVersionedWorkflow(
      @ApiParam(value = "Workflow's token to authenticate the request", required = true)
      @RequestHeader(name = X_MANAGEMENT_TOKEN_KEY) String token,
      @ApiParam(value = "Workflow's id that is provided in SWADL", required = true) @PathVariable String workflowId,
      @ApiParam(value = "Get a specific version value") @RequestParam(required = false, name = "version")
      Long version,
      @ApiParam(value = "Get all versions flag", defaultValue = "false")
      @RequestParam(required = false, name = "all_versions", defaultValue = "false") Boolean allVersions);

  @ApiOperation(
      "Delete the workflow by the given ID, all versions will be deleted, unless the version parameter is specified.")
  @ApiResponses(value = {@ApiResponse(code = 204, message = ""),
      @ApiResponse(code = 404, message = "No workflow found with id {id}", response = ErrorResponse.class),
      @ApiResponse(code = 401, message = "Request is not authorised", response = ErrorResponse.class)})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping(path = "/{workflowId}")
  ResponseEntity<Void> deleteWorkflowByIdAndVersion(
      @ApiParam(value = "Workflow's token to authenticate the request", required = true)
      @RequestHeader(name = X_MANAGEMENT_TOKEN_KEY) String token,
      @ApiParam(value = "Workflow's id that is provided in SWADL", required = true) @PathVariable String workflowId,
      @ApiParam(value = "Workflow's version to delete") @RequestParam(required = false, name = "version")
      Long version);

  @ApiOperation("Fall back to a specific workflow version, or set an expiration time for the workflow.")
  @ApiResponses(value = {@ApiResponse(code = 204, message = ""),
      @ApiResponse(code = 404, message = "No workflow found with id {id} and version {version}",
          response = ErrorResponse.class),
      @ApiResponse(code = 401, message = "Request is not authorised", response = ErrorResponse.class)})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PutMapping(path = "/{workflowId}")
  ResponseEntity<Void> setVersionAndExpirationTime(
      @ApiParam(value = "Workflow's token to authenticate the request", required = true)
      @RequestHeader(name = X_MANAGEMENT_TOKEN_KEY) String token,
      @ApiParam(value = "Workflow's id that is provided in SWADL", required = true) @PathVariable String workflowId,
      @ApiParam(value = "Workflow's version to roll back to") @RequestParam(required = false, name = "version")
      Long version,
      @ApiParam(value = "Expiration date. Instant epoch.") @RequestParam(required = false, name = "expiration_date")
      Instant expirationDate);

  @ApiOperation("Streaming logs in SSE.")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "", response = ResponseBodyEmitter.class),
      @ApiResponse(code = 401, message = "Request is not authorised", response = ErrorResponse.class)})
  @ResponseStatus(HttpStatus.OK)
  @GetMapping(path = "/logs", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  SseEmitter streamingLogs(
      @ApiParam(value = "Workflow's token to authenticate the request", required = true)
      @RequestHeader(name = X_MANAGEMENT_TOKEN_KEY) String token);

  @ApiOperation("Upload a secret")
  @ApiResponses(value = {@ApiResponse(code = 204, message = "")})
  @PostMapping("/secrets")
  ResponseEntity<Void> uploadSecret(@Valid @RequestBody SecretView secrete);

  @ApiOperation("Delete a secret")
  @ApiResponses(value = {@ApiResponse(code = 204, message = "")})
  @DeleteMapping("/secrets/{key}")
  ResponseEntity<Void> deleteSecret(@PathVariable("key") String secretKey);

  @ApiOperation("Get secrets")
  @ApiResponses(value = {@ApiResponse(code = 204, message = "")})
  @GetMapping(value = "/secrets", produces = "application/json")
  ResponseEntity<List<SecretKeeper.SecretMetadata>> getSecretMetadata();
}
