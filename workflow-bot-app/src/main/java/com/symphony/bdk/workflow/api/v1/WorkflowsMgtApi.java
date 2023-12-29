package com.symphony.bdk.workflow.api.v1;

import com.symphony.bdk.workflow.api.v1.dto.SecretView;
import com.symphony.bdk.workflow.api.v1.dto.SwadlView;
import com.symphony.bdk.workflow.api.v1.dto.VersionedWorkflowView;
import com.symphony.bdk.workflow.engine.executor.SecretKeeper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import javax.validation.Valid;

@Tag(name = "Api to manage workflow swadl lifecycle")
public interface WorkflowsMgtApi {
  String X_MANAGEMENT_TOKEN_KEY = "X-Management-Token";

  @Operation(description = "Validate and save/deploy a new workflow SWADL content.")
  @ApiResponses(value = {@ApiResponse(responseCode = "204"),
      @ApiResponse(responseCode = "400", description = "Invalid workflow swadl"),
      @ApiResponse(responseCode = "401", description = "Request is not authorised")})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<Void> saveAndDeploySwadl(
      @Parameter(description = "Workflow's token to authenticate the request", required = true)
      @RequestHeader(name = X_MANAGEMENT_TOKEN_KEY) String token,
      @Parameter(description = "Workflow SWADL form to create") @Valid @ModelAttribute SwadlView swadlView);

  @Operation(description = "Validate and update the latest workflow SWADL content.")
  @ApiResponses(value = {@ApiResponse(responseCode = "204"),
      @ApiResponse(responseCode = "400", description = "Invalid workflow swadl"),
      @ApiResponse(responseCode = "404", description = "No workflow found with id {id}"),
      @ApiResponse(responseCode = "401", description = "Request is not authorised")})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<Void> updateSwadl(
      @Parameter(description = "Workflow's token to authenticate the request", required = true)
      @RequestHeader(name = X_MANAGEMENT_TOKEN_KEY) String token,
      @Parameter(description = "Workflow SWADL form to update") @Valid @ModelAttribute SwadlView swadlView);

  @Operation(description =
      "Get the active version of the workflow by the given ID, which is default behavior; if the version parameter"
          + "is given, that version will be returned; if all_versions flag is true, all versions will be returned; if"
          + "version is provided and versions flag is true, all versions will be returned.")
  @ApiResponses(value = {@ApiResponse(responseCode = "204"),
      @ApiResponse(responseCode = "404", description = "No workflow found with id {id}"),
      @ApiResponse(responseCode = "401", description = "Request is not authorised")})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @GetMapping(path = "/{workflowId}", produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<List<VersionedWorkflowView>> getVersionedWorkflow(
      @Parameter(description = "Workflow's token to authenticate the request", required = true)
      @RequestHeader(name = X_MANAGEMENT_TOKEN_KEY) String token,
      @Parameter(description = "Workflow's id that is provided in SWADL", required = true) @PathVariable String workflowId,
      @Parameter(description = "Get a specific version value") @RequestParam(required = false, name = "version")
      Long version,
      @Parameter(description = "Get all versions flag")
      @RequestParam(required = false, name = "all_versions", defaultValue = "false") Boolean allVersions);

  @Operation(description =
      "Delete the workflow by the given ID, all versions will be deleted, unless the version parameter is specified.")
  @ApiResponses(value = {@ApiResponse(responseCode = "204"),
      @ApiResponse(responseCode = "404", description = "No workflow found with id {id}"),
      @ApiResponse(responseCode = "401", description = "Request is not authorised")})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping(path = "/{workflowId}")
  ResponseEntity<Void> deleteWorkflowByIdAndVersion(
      @Parameter(description = "Workflow's token to authenticate the request", required = true)
      @RequestHeader(name = X_MANAGEMENT_TOKEN_KEY) String token,
      @Parameter(description = "Workflow's id that is provided in SWADL", required = true) @PathVariable String workflowId,
      @Parameter(description = "Workflow's version to delete") @RequestParam(required = false, name = "version")
      Long version);

  @Operation(description = "Fall back to a specific workflow version, or set an expiration time for the workflow.")
  @ApiResponses(value = {@ApiResponse(responseCode = "204"),
      @ApiResponse(responseCode = "404", description = "No workflow found with id {id} and version {version}"),
      @ApiResponse(responseCode = "401", description = "Request is not authorised")})
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PutMapping(path = "/{workflowId}")
  ResponseEntity<Void> setVersionAndExpirationTime(
      @Parameter(description = "Workflow's token to authenticate the request", required = true)
      @RequestHeader(name = X_MANAGEMENT_TOKEN_KEY) String token,
      @Parameter(description = "Workflow's id that is provided in SWADL", required = true) @PathVariable String workflowId,
      @Parameter(description = "Workflow's version to roll back to") @RequestParam(required = false, name = "version")
      Long version,
      @Parameter(description = "Expiration date. Instant epoch.") @RequestParam(required = false, name = "expiration_date")
      Instant expirationDate);

  @Operation(description = "Streaming logs in SSE.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200"),
      @ApiResponse(responseCode = "401", description = "Request is not authorised")})
  @ResponseStatus(HttpStatus.OK)
  @GetMapping(path = "/logs", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  SseEmitter streamingLogs(
      @Parameter(description = "Workflow's token to authenticate the request", required = true)
      @RequestHeader(name = X_MANAGEMENT_TOKEN_KEY) String token);

  @Operation(description = "Upload a secret")
  @ApiResponses(value = {@ApiResponse(responseCode = "204")})
  @PostMapping("/secrets")
  ResponseEntity<Void> uploadSecret(@Valid @RequestBody SecretView secrete);

  @Operation(description = "Delete a secret")
  @ApiResponses(value = {@ApiResponse(responseCode = "204")})
  @DeleteMapping("/secrets/{key}")
  ResponseEntity<Void> deleteSecret(@PathVariable("key") String secretKey);

  @Operation(description = "Get secrets")
  @ApiResponses(value = {@ApiResponse(responseCode = "204")})
  @GetMapping(value = "/secrets", produces = "application/json")
  ResponseEntity<List<SecretKeeper.SecretMetadata>> getSecretMetadata();
}
