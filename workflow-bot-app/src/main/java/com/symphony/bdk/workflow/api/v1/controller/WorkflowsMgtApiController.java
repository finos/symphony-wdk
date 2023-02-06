package com.symphony.bdk.workflow.api.v1.controller;

import com.symphony.bdk.workflow.api.v1.WorkflowsMgtApi;
import com.symphony.bdk.workflow.api.v1.dto.SwadlView;
import com.symphony.bdk.workflow.api.v1.dto.VersionedWorkflowView;
import com.symphony.bdk.workflow.configuration.ConditionalOnPropertyNotEmpty;
import com.symphony.bdk.workflow.exception.NotFoundException;
import com.symphony.bdk.workflow.expiration.WorkflowExpirationService;
import com.symphony.bdk.workflow.logs.LogsStreamingService;
import com.symphony.bdk.workflow.management.WorkflowManagementService;
import com.symphony.bdk.workflow.security.Authorized;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RestController
@ConditionalOnPropertyNotEmpty("wdk.properties.management-token")
@RequestMapping("/v1/management/workflows")
@RequiredArgsConstructor
@Slf4j
public class WorkflowsMgtApiController implements WorkflowsMgtApi {
  private final WorkflowManagementService workflowManagementService;
  private final WorkflowExpirationService workflowExpirationService;
  private final LogsStreamingService logsStreamingService;

  @Override
  @Authorized(headerTokenKey = X_MANAGEMENT_TOKEN_KEY)
  public ResponseEntity<Void> saveAndDeploySwadl(String token, SwadlView swadlView) {
    workflowManagementService.deploy(swadlView);
    return ResponseEntity.noContent().build();
  }

  @Override
  @Authorized(headerTokenKey = X_MANAGEMENT_TOKEN_KEY)
  public ResponseEntity<Void> updateSwadl(String token, SwadlView swadlView) {
    workflowManagementService.update(swadlView);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<List<VersionedWorkflowView>> getSwadl(String token, String id) {
    return ResponseEntity.ok(workflowManagementService.get(id));
  }

  @Override
  @Authorized(headerTokenKey = X_MANAGEMENT_TOKEN_KEY)
  public ResponseEntity<Void> deleteSwadl(String token, String id) {
    workflowManagementService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  @Authorized(headerTokenKey = X_MANAGEMENT_TOKEN_KEY)
  public ResponseEntity<Void> deployActiveVersion(String token, String id, Long version) {
    workflowManagementService.setActiveVersion(id, version);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<VersionedWorkflowView> getSwadlByVersion(String token, String id, Long version) {
    Optional<VersionedWorkflowView> workflowView = workflowManagementService.get(id, version);
    return workflowView.map(ResponseEntity::ok)
        .orElseThrow(
            () -> new NotFoundException(String.format("Version %s of workflow %s is not found.", version, id)));
  }

  @Override
  public ResponseEntity<Void> deleteSwadlByVersion(String token, String id, Long version) {
    workflowManagementService.delete(id, version);
    return ResponseEntity.noContent().build();
  }

  @Override
  @Authorized(headerTokenKey = X_MANAGEMENT_TOKEN_KEY)
  public SseEmitter streamingLogs(String token) {
    SseEmitter emitter = new SseEmitter();
    logsStreamingService.subscribe(emitter);
    return emitter;
  }

  @Override
  @Authorized(headerTokenKey = X_MANAGEMENT_TOKEN_KEY)
  public ResponseEntity<Void> scheduleWorkflowExpirationJob(String workflowId, Instant expirationDate) {
    workflowExpirationService.scheduleWorkflowExpiration(workflowId, expirationDate);
    return ResponseEntity.ok().build();
  }
}
