package com.symphony.bdk.workflow.api.v1.controller;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphony.bdk.workflow.api.v1.WorkflowsMgtApi;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowMgtAction;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.management.WorkflowsMgtAction;
import com.symphony.bdk.workflow.management.WorkflowsMgtActionHolder;
import com.symphony.bdk.workflow.monitoring.service.MonitoringService;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

@WebMvcTest
class WorkflowsMgtApiControllerTest {

  private static final String URL = "/v1/management/workflows/";

  @Autowired
  MockMvc mockMvc;

  @MockBean
  WorkflowsMgtActionHolder mgtActionHolder;

  @MockBean
  WorkflowEngine<BpmnModelInstance> engine;

  @MockBean
  protected MonitoringService monitoringService;

  static Stream<Arguments> argumentsStream() {
    return Stream.of(
        arguments(WorkflowMgtAction.DEPLOY, HttpMethod.POST, URL),
        arguments(WorkflowMgtAction.UPDATE, HttpMethod.PUT, URL),
        arguments(WorkflowMgtAction.DELETE, HttpMethod.DELETE, URL + "id")
    );
  }

  @ParameterizedTest
  @MethodSource("argumentsStream")
  void test_managementRequests_ok(WorkflowMgtAction action, HttpMethod method, String url) throws Exception {
    WorkflowsMgtAction mock = mock(WorkflowsMgtAction.class);
    when(mgtActionHolder.getInstance(eq(action))).thenReturn(mock);
    doNothing().when(mock).doAction(anyString());

    mockMvc.perform(request(method, url)
            .header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken")
            .contentType("text/plain")
            .content("swadl content"))
        .andExpect(status().isNoContent());

    verify(mock).doAction(anyString());
  }

  @Test
  void test_missingToken_badRequest() throws Exception {
    mockMvc.perform(request(HttpMethod.POST, URL)
            .contentType("text/plain")
            .content("content"))
        .andExpect(status().isBadRequest());
  }
}
