package com.symphony.bdk.workflow.api.v1.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphony.bdk.workflow.api.v1.WorkflowsMgtApi;
import com.symphony.bdk.workflow.api.v1.dto.SwadlView;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.Optional;

class WorkflowsMgtApiControllerTest extends ApiTest {

  private static final String URL = "/v1/management/workflows/";

  @Test
  void test_managementRequests_deploy() throws Exception {
    doNothing().when(workflowManagementService).deploy(any(SwadlView.class));

    mockMvc.perform(request(HttpMethod.POST, URL)
            .header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken")
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .param("swadl", "content")
            .param("description", "description"))
        .andExpect(status().isNoContent());

    verify(workflowManagementService).deploy(any(SwadlView.class));
  }

  @Test
  void test_managementRequests_update() throws Exception {
    doNothing().when(workflowManagementService).update(any(SwadlView.class));

    mockMvc.perform(request(HttpMethod.PUT, URL)
            .header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken")
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .param("swadl", "content")
            .param("description", "description"))
        .andExpect(status().isNoContent());

    verify(workflowManagementService).update(any(SwadlView.class));
  }

  @Test
  void test_managementRequests_delete() throws Exception {
    doNothing().when(workflowManagementService).delete(anyString(), any(Optional.class));

    mockMvc.perform(request(HttpMethod.DELETE, URL + "id")
            .header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken")
            .contentType("text/plain"))
        .andExpect(status().isNoContent());

    verify(workflowManagementService).delete(anyString(), any(Optional.class));
  }

  @Test
  void test_missingToken_badRequest() throws Exception {
    mockMvc.perform(request(HttpMethod.POST, URL)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .param("swadl", "content")
            .param("description", "description"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void test_streamingLogs_missingToken_badRequest() throws Exception {
    mockMvc.perform(request(HttpMethod.GET, URL + "/logs")
            .contentType("text/plain"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void test_streamingLogs_returnOk() throws Exception {
    mockMvc.perform(request(HttpMethod.GET, URL + "/logs")
            .contentType("text/plain")
            .header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken"))
        .andExpect(status().isOk());
  }

  @Test
  void test_setActiveVersion_badRequest() throws Exception {
    mockMvc.perform(request(HttpMethod.POST, URL + "/wfid/versions/v1")
            .contentType("text/plain")
            .content("content"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void test_setActiveVersion_returnOk() throws Exception {
    doNothing().when(workflowManagementService).setActiveVersion(anyString(), anyLong());

    mockMvc.perform(request(HttpMethod.POST, URL + "/wfId/versions/1674651222294886")
            .header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken")
            .contentType("text/plain"))
        .andExpect(status().isNoContent());

    verify(workflowManagementService).setActiveVersion(eq("wfId"), eq(1674651222294886L));

  }
}
