package com.symphony.bdk.workflow.api.v1.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphony.bdk.workflow.api.v1.WorkflowsMgtApi;
import com.symphony.bdk.workflow.api.v1.dto.SwadlView;
import com.symphony.bdk.workflow.api.v1.dto.VersionedWorkflowView;
import com.symphony.bdk.workflow.engine.executor.SecretKeeper;
import com.symphony.bdk.workflow.exception.NotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@DisplayName("Test workflow management api")
class WorkflowsMgtApiControllerTest extends ApiTest {

  private static final String URL = "/v1/workflows";

  private static final String SECRET = "/v1/workflows/secrets";


  @Nested
  @DisplayName("Deployment api")
  class Deployment {
    @Test
    @DisplayName("Save deployment")
    void testDeploy() throws Exception {
      doNothing().when(workflowManagementService).deploy(any(SwadlView.class));

      mockMvc.perform(post(URL).header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken")
          .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
          .param("swadl", "content")
          .param("description", "description")).andExpect(status().isNoContent());

      verify(workflowManagementService).deploy(any(SwadlView.class));
    }

    @Test
    @DisplayName("Update deployment")
    void testUpdate() throws Exception {
      doNothing().when(workflowManagementService).update(any(SwadlView.class));

      mockMvc.perform(put(URL).header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken")
          .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
          .param("swadl", "content")
          .param("description", "description")).andExpect(status().isNoContent());

      verify(workflowManagementService).update(any(SwadlView.class));
    }

    @Test
    @DisplayName("Missing token bad request")
    void test_missingToken_badRequest() throws Exception {
      mockMvc.perform(post(URL).contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
          .param("swadl", "content")
          .param("description", "description")).andExpect(status().isBadRequest());
    }
  }


  @Nested
  @DisplayName("Get workflows")
  class GetMethods {
    String getUri = URL + "/{workflowId}";

    @Test
    @DisplayName("Get active version")
    void testGetActiveVersion() throws Exception {
      when(workflowManagementService.get(anyString())).thenReturn(Optional.of(VersionedWorkflowView.builder().build()));

      mockMvc.perform(get(getUri, "id").header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken"))
          .andExpect(status().isOk());

      verify(workflowManagementService).get(anyString());
    }

    @Test
    @DisplayName("Get not found version")
    void testGetNotFoundVersion() throws Exception {
      when(workflowManagementService.get(anyString(), anyLong())).thenReturn(Optional.empty());

      mockMvc.perform(get(getUri, "/wfId").header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken")
          .queryParam("version", "1674651222294886")).andExpect(status().isOk());

      verify(workflowManagementService).get(eq("wfId"), eq(1674651222294886L));
    }

    @Test
    @DisplayName("Get specific version")
    void testGetSpecificVersion() throws Exception {
      when(workflowManagementService.get(anyString(), anyLong())).thenReturn(
          Optional.of(VersionedWorkflowView.builder().build()));

      mockMvc.perform(get(getUri, "/wfId").header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken")
          .queryParam("version", "1674651222294886")).andExpect(status().isOk());

      verify(workflowManagementService).get(eq("wfId"), eq(1674651222294886L));
    }

    @Test
    @DisplayName("Get all versions")
    void testAllVersions() throws Exception {
      when(workflowManagementService.getAllVersions(anyString())).thenReturn(
          List.of(VersionedWorkflowView.builder().build()));

      mockMvc.perform(get(getUri, "/wfId").header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken")
          .queryParam("all_versions", "true")).andExpect(status().isOk());

      verify(workflowManagementService).getAllVersions(eq("wfId"));
    }
  }


  @Nested
  @DisplayName("Delete workflow")
  class DeleteMethods {
    String deleteUri = URL + "/{workflowId}";

    @Test
    @DisplayName("by ID")
    void testDeleteWorkflow() throws Exception {
      doNothing().when(workflowManagementService).delete(anyString());

      mockMvc.perform(delete(deleteUri, "id").header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken")
          .contentType("application/json")).andExpect(status().isNoContent());

      verify(workflowManagementService).delete(anyString());
    }

    @Test
    @DisplayName("by version")
    void testDeleteByVersion() throws Exception {
      doNothing().when(workflowManagementService).delete(anyString(), anyLong());

      mockMvc.perform(delete(deleteUri, "id").header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken")
              .contentType("application/json")
              .queryParam("version", "1674651222294886"))
          .andExpect(status().isNoContent());

      verify(workflowManagementService).delete(anyString(), anyLong());
    }
  }


  @Nested
  @DisplayName("Test Streaming logs")
  class StreamLog {
    String streamLogUri = URL + "/logs";

    @Test
    @DisplayName("Streaming logs missing token")
    void test_streamingLogs_missingToken_badRequest() throws Exception {
      mockMvc.perform(get(streamLogUri).contentType("text/plain")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Streaming logs")
    void test_streamingLogs_returnOk() throws Exception {
      mockMvc.perform(
              get(streamLogUri).contentType("text/plain").header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken"))
          .andExpect(status().isOk());
    }
  }


  @Nested
  @DisplayName("Set active version and expiration time of workflow")
  class ActiveVersion {
    String getVersionUri = URL + "/{workflowId}";

    @Test
    @DisplayName("Set active version successfully")
    void testActiveVersion() throws Exception {
      doNothing().when(workflowManagementService).setActiveVersion(anyString(), anyLong());

      mockMvc.perform(put(getVersionUri, "/wfId").header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken")
          .queryParam("version", "1674651222294886")
          .contentType("application/json")).andExpect(status().isNoContent());

      verify(workflowManagementService).setActiveVersion(eq("wfId"), eq(1674651222294886L));
    }

    @Test
    @DisplayName("Set active version and expiration time")
    void testActiveVersionAndExpiration() throws Exception {
      doNothing().when(workflowManagementService).setActiveVersion(anyString(), anyLong());
      final Instant now = Instant.now();
      doNothing().when(workflowExpirationService).scheduleWorkflowExpiration(eq("wfId"), eq(now));

      mockMvc.perform(put(getVersionUri, "/wfId").header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken")
              .contentType("application/json")
              .queryParam("version", "1674651222294886")
              .queryParam("expiration_date", now.toString()))
          .andExpect(status().isNoContent());

      verify(workflowManagementService).setActiveVersion(eq("wfId"), eq(1674651222294886L));
      verify(workflowExpirationService).scheduleWorkflowExpiration(eq("wfId"), eq(now));
    }

    @Test
    @DisplayName("Token is missing")
    void testTokenMissing() throws Exception {
      mockMvc.perform(put(getVersionUri, "/wfId").contentType("application/json")
              .queryParam("expiration_date", Instant.now().toString()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Workflow not found")
    void testWorkflowNotFound() throws Exception {
      final String workflowId = "wfId";
      final Instant now = Instant.now();

      doThrow(NotFoundException.class).when(workflowExpirationService)
          .scheduleWorkflowExpiration(eq(workflowId), eq(now));

      mockMvc.perform(put(getVersionUri, workflowId).header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken")
              .contentType("application/json")
              .queryParam("expiration_date", now.toString()))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Schedule workflow expiration successfully")
    void testScheduleWorkflowExpiration() throws Exception {
      final String workflowId = "wfId";
      final Instant now = Instant.now();

      doNothing().when(workflowExpirationService).scheduleWorkflowExpiration(eq(workflowId), eq(now));
      mockMvc.perform(put(getVersionUri, workflowId).header(WorkflowsMgtApi.X_MANAGEMENT_TOKEN_KEY, "myToken")
              .contentType("application/json")
              .queryParam("expiration_date", now.toString()))
          .andExpect(status().isNoContent());

      verify(workflowExpirationService).scheduleWorkflowExpiration(eq(workflowId), eq(now));
    }
  }

  @Nested
  @DisplayName("Secret management test")
  class SecretManagement {
    @Test
    void uploadSecret() throws Exception {
      doNothing().when(secretKeeper).save(anyString(), any());

      mockMvc.perform(request(HttpMethod.POST, SECRET).contentType("application/json")
              .content("{\"key\": \"myKey\", \"secret\": \"my secret\"}"))
          .andExpect(status().isNoContent());
    }

    @Test
    void removeSecret() throws Exception {
      doNothing().when(secretKeeper).remove(anyString());
      mockMvc.perform(request(HttpMethod.DELETE, SECRET + "/myKey")).andExpect(status().isNoContent());
    }

    @Test
    void getSecretMetadata() throws Exception {
      when(secretKeeper.getSecretsMetadata()).thenReturn(
          List.of(new SecretKeeper.SecretMetadata("ref", Instant.now())));
      mockMvc.perform(request(HttpMethod.GET, SECRET).accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("[0].secretKey").value("ref"));
    }
  }

}
