package com.symphony.bdk.workflow.engine.executor.request;

import com.symphony.bdk.http.api.ApiException;
import com.symphony.bdk.http.api.ApiResponse;
import com.symphony.bdk.http.api.util.TypeReference;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.swadl.v1.activity.request.ExecuteRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class RequestExecutor implements ActivityExecutor<ExecuteRequest> {

  private static final String OUTPUT_STATUS_KEY = "status";
  private static final String OUTPUT_BODY_KEY = "body";

  private Map<String, String> headersToString(Map<String, Object> headers) {
    Map<String, String> result = new HashMap<>();
    for (Map.Entry<String, Object> entry : headers.entrySet()) {

      // We consider that opening/closing brackets are not allowed in headers values.
      // Otherwise, the list items should be added in the string one by on using map function
      String listToString =
          String.join(",", List.of(entry.getValue()).toString().replace("[", "").replace("]", ""));
      result.put(entry.getKey(), String.join(",", listToString));
    }
    return result;
  }

  @Override
  public void execute(ActivityExecutorContext<ExecuteRequest> execution) throws JsonProcessingException {
    ExecuteRequest activity = execution.getActivity();
    Object data;
    int statusCode;

    try {
      ApiResponse<Object> apiResponse = execution.bdk()
          .apiClient(activity.getUrl())
          .invokeAPI("", activity.getMethod(), Collections.emptyList(), activity.getBody(),
              headersToString(activity.getHeaders()),
              Collections.emptyMap(), Collections.emptyMap(), "application/json", "application/json", null,
              new TypeReference<>() {});

      data = apiResponse.getData();
      statusCode = apiResponse.getStatusCode();
    } catch (ApiException apiException) {
      statusCode = apiException.getCode();

      // we need to set the responseBody as a map instead of a string to allow processing it in subsequent activities
      try {
        data = new ObjectMapper().readValue(apiException.getResponseBody(), Map.class);
      } catch (JsonProcessingException e) {
        data = new ObjectMapper().readValue(String.format("{\"message\": \"%s\"}", apiException.getResponseBody()),
            Map.class);
      }
      log.debug("This error happens when the request fails.", apiException);
    }

    execution.setOutputVariable(OUTPUT_STATUS_KEY, statusCode);
    execution.setOutputVariable(OUTPUT_BODY_KEY, data);
  }
}
