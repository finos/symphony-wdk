package com.symphony.bdk.workflow.engine.executor.request;

import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.request.client.HttpClient;
import com.symphony.bdk.workflow.engine.executor.request.client.Response;
import com.symphony.bdk.workflow.swadl.v1.activity.request.ExecuteRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RequestExecutor implements ActivityExecutor<ExecuteRequest> {

  private static final String OUTPUT_STATUS_KEY = "status";
  private static final String OUTPUT_BODY_KEY = "body";

  private final HttpClient httpClient;

  public RequestExecutor(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Override
  public void execute(ActivityExecutorContext<ExecuteRequest> execution) throws JsonProcessingException {
    ExecuteRequest activity = execution.getActivity();
    Object data;
    int statusCode;

    try {
      Response response =
          this.httpClient.execute(activity.getMethod(), activity.getUrl(), activity.getBody(),
              headersToString(activity.getHeaders()));

      statusCode = response.getCode();


      if (HttpStatus.valueOf(statusCode).is2xxSuccessful()) {
        data = response.getContent();
      } else {
        // we need to set the responseBody as a map instead of a string to allow processing it in subsequent activities
        data = this.handleException(response.getContent());
        log.debug("This error happens when the request fails. {}", response.getContent());
      }

    } catch (IOException ioException) {
      statusCode = 500;
      data = this.handleException(ioException.getMessage());
    }

    execution.setOutputVariable(OUTPUT_STATUS_KEY, statusCode);
    execution.setOutputVariable(OUTPUT_BODY_KEY, data);
  }

  private Object handleException(String message) throws JsonProcessingException {
    Object data;
    try {
      data = new ObjectMapper().readValue(message, Map.class);
    } catch (JsonProcessingException e) {
      data = new ObjectMapper().readValue(String.format("{\"message\": \"%s\"}", message),
          Map.class);
    }

    return data;
  }

  private Map<String, String> headersToString(Map<String, Object> headers) {
    Map<String, String> result = new HashMap<>();
    for (Map.Entry<String, Object> entry : headers.entrySet()) {

      // We consider that opening/closing brackets are not allowed in headers values.
      // Otherwise, the list items should be added in the string one by one using map function
      String listToString =
          String.join(",", List.of(entry.getValue()).toString().replace("[", "").replace("]", ""));
      result.put(entry.getKey(), String.join(",", listToString));
    }
    return result;
  }
}
