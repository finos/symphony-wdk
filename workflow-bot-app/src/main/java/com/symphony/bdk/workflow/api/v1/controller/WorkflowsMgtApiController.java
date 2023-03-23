package com.symphony.bdk.workflow.api.v1.controller;

import com.symphony.bdk.workflow.api.v1.WorkflowsMgtApi;
import com.symphony.bdk.workflow.api.v1.dto.SwadlView;
import com.symphony.bdk.workflow.api.v1.dto.VersionedWorkflowView;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@ConditionalOnPropertyNotEmpty("wdk.properties.management-token")
@RequestMapping("/v1/workflows")
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
  public ResponseEntity<List<VersionedWorkflowView>> getVersionedWorkflow(String token, String workflowId, Long version,
      Boolean allVersions) {
    List<VersionedWorkflowView> result = new ArrayList<>();
    if (version == null && !allVersions) {
      workflowManagementService.get(workflowId).ifPresent(result::add);
    } else if (allVersions) {
      result.addAll(workflowManagementService.getAllVersions(workflowId));
    } else {
      workflowManagementService.get(workflowId, version).ifPresent(result::add);
    }
    return ResponseEntity.ok(result);
  }

  @Override
  @Authorized(headerTokenKey = X_MANAGEMENT_TOKEN_KEY)
  public ResponseEntity<Void> deleteWorkflowByIdAndVersion(String token, String workflowId, Long version) {
    Optional.ofNullable(version)
            .ifPresentOrElse(v -> workflowManagementService.delete(workflowId, version),
                () -> workflowManagementService.delete(workflowId));
    return ResponseEntity.noContent().build();
  }

  @Override
  @Authorized(headerTokenKey = X_MANAGEMENT_TOKEN_KEY)
  public ResponseEntity<Void> setVersionAndExpirationTime(String token, String workflowId, Long version,
      Instant expirationDate) {
    if (version != null) {
      workflowManagementService.setActiveVersion(workflowId, version);
    }

    if (expirationDate != null) {
      workflowExpirationService.scheduleWorkflowExpiration(workflowId, expirationDate);
    }
    return ResponseEntity.noContent().build();
  }

  @Override
  @Authorized(headerTokenKey = X_MANAGEMENT_TOKEN_KEY)
  public SseEmitter streamingLogs(String token) {
    SseEmitter emitter = new SseEmitter();
    logsStreamingService.subscribe(emitter);
    return emitter;
  }
}
