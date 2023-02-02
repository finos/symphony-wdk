package com.symphony.bdk.workflow.api.v1.controller;

import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.engine.camunda.CamundaTranslatedWorkflowContext;
import com.symphony.bdk.workflow.expiration.WorkflowExpirationService;
import com.symphony.bdk.workflow.logs.LogsStreamingService;
import com.symphony.bdk.workflow.management.WorkflowManagementService;
import com.symphony.bdk.workflow.monitoring.service.MonitoringService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(properties = {"wdk.properties.management-token=myToken"})
public class ApiTest {
  @Autowired
  MockMvc mockMvc;

  @MockBean
  WorkflowEngine<CamundaTranslatedWorkflowContext> engine;

  @MockBean
  MonitoringService monitoringService;

  @MockBean
  LogsStreamingService logsStreamingService;

  @MockBean
  WorkflowManagementService workflowManagementService;

  @MockBean WorkflowExpirationService workflowExpirationService;
}
