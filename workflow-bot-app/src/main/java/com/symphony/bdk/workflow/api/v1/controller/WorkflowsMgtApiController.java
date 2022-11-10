package com.symphony.bdk.workflow.api.v1.controller;

import com.symphony.bdk.workflow.api.v1.WorkflowsMgtApi;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowMgtAction;
import com.symphony.bdk.workflow.management.WorkflowsMgtActionHolder;
import com.symphony.bdk.workflow.security.Authorized;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/management/workflows")
@RequiredArgsConstructor
@Slf4j
public class WorkflowsMgtApiController implements WorkflowsMgtApi {
  private final WorkflowsMgtActionHolder mgtActionHolder;

  @Override
  @Authorized(headerTokenKey = X_MANAGEMENT_TOKEN_KEY)
  public ResponseEntity<Void> deploySwadl(String token, String content) {
    mgtActionHolder.getInstance(WorkflowMgtAction.DEPLOY).doAction(content);
    return ResponseEntity.noContent().build();
  }

  @Override
  @Authorized(headerTokenKey = X_MANAGEMENT_TOKEN_KEY)
  public ResponseEntity<Void> updateSwadl(String token, String content) {
    mgtActionHolder.getInstance(WorkflowMgtAction.UPDATE).doAction(content);
    return ResponseEntity.noContent().build();
  }

  @Override
  @Authorized(headerTokenKey = X_MANAGEMENT_TOKEN_KEY)
  public ResponseEntity<Void> deleteSwadlById(String token, String id) {
    mgtActionHolder.getInstance(WorkflowMgtAction.DELETE).doAction(id);
    return ResponseEntity.noContent().build();
  }
}
