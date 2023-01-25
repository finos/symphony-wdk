package com.symphony.bdk.workflow.api.v1.controller;

import com.symphony.bdk.workflow.api.v1.WorkflowsMgtApi;
import com.symphony.bdk.workflow.configuration.ConditionalOnPropertyNotEmpty;
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
  public ResponseEntity<Void> deploySwadl(String token, String content) {
    workflowManagementService.deploy(content);
    return ResponseEntity.noContent().build();
  }

  @Override
  @Authorized(headerTokenKey = X_MANAGEMENT_TOKEN_KEY)
  public ResponseEntity<Void> updateSwadl(String token, String content) {
    workflowManagementService.update(content);
    return ResponseEntity.noContent().build();
  }

  @Override
  @Authorized(headerTokenKey = X_MANAGEMENT_TOKEN_KEY)
  public ResponseEntity<Void> deleteSwadlById(String token, String id) {
    workflowManagementService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  @Authorized(headerTokenKey = X_MANAGEMENT_TOKEN_KEY)
  public ResponseEntity<Void> setActiveVersion(String token, String id, String version) {
    workflowManagementService.setActiveVersion(id, version);
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
  public ResponseEntity<Void> addWorkflowExpirationJob(String workflowId, String type, Instant expirationDate) {
    workflowExpirationService.scheduleWorkflowExpiration(workflowId, type, expirationDate);
    return ResponseEntity.ok().build();
  }
}
