package com.symphony.bdk.workflow.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphony.bdk.workflow.engine.ExecutionParameters;
import com.symphony.bdk.workflow.engine.UnauthorizedException;
import com.symphony.bdk.workflow.engine.WorkflowEngine;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.util.NestedServletException;

@WebMvcTest
class WorkflowsApiControllerIntegrationTest {

  private static final String PATH = "/v1/workflows/wfId/execute";

  @Autowired
  protected MockMvc mockMvc;

  @MockBean
  protected WorkflowEngine workflowEngine;

  @Test
  void executeWorkflowById_validRequestTest() throws Exception {
    doNothing().when(workflowEngine)
        .execute(eq("wfId"), any(ExecutionParameters.class));

    MvcResult mvcResult = mockMvc.perform(request(HttpMethod.POST, PATH)
            .header("X-Workflow-Token", "myToken")
            .contentType("application/json")
            .content("{\"arguments\": {\"content\":\"hello\"}}"))
        .andExpect(status().isNoContent())
        .andReturn();

    assertThat(mvcResult.getResponse().getContentAsString()).isEmpty();
  }

  @Test
  void executeWorkflowById_noTokenProvidedTest() throws Exception {
    mockMvc.perform(request(HttpMethod.POST, PATH)
            .contentType("application/json")
            .content("{\"args\": {\"content\":\"hello\"}}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void executeWorkflowById_runTimeExceptionTest() throws Exception {
    doThrow(new RuntimeException("Error parsing presentationML")).when(workflowEngine)
        .execute(eq("wfId"), any(ExecutionParameters.class));

    MockHttpServletRequestBuilder request = request(HttpMethod.POST, PATH).header("X-Workflow-Token", "myToken")
        .contentType("application/json")
        .content("{\"args\": {\"content\":\"hello\"}}");
    try {
      mockMvc.perform(request);
      fail("A RuntimeException should have been thrown");
    } catch (Exception exception) {
      assertThat(exception).isInstanceOf(NestedServletException.class);
      assertThat(exception.getMessage()).isEqualTo(
          "Request processing failed; nested exception is java.lang.RuntimeException: Error parsing presentationML");
    }

  }

  @Test
  void executeWorkflowById_illegalArgumentExceptionTest() throws Exception {
    doThrow(new IllegalArgumentException("No workflow found with id wfId")).when(workflowEngine)
        .execute(eq("wfId"), any(ExecutionParameters.class));

    mockMvc.perform(request(HttpMethod.POST, PATH)
            .header("X-Workflow-Token", "myToken")
            .contentType("application/json")
            .content("{\"args\": {\"content\":\"hello\"}}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("No workflow found with id wfId"));
  }

  @Test
  void executeWorkflowById_unauthorizedExceptionTest() throws Exception {
    doThrow(new UnauthorizedException("Token is not valid")).when(workflowEngine)
        .execute(eq("wfId"), any(ExecutionParameters.class));

    mockMvc.perform(request(HttpMethod.POST, PATH)
            .header("X-Workflow-Token", "myToken")
            .contentType("application/json")
            .content("{\"args\": {\"content\":\"hello\"}}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Token is not valid"));
  }
}
