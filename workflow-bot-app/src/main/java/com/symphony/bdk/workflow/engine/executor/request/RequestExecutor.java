package com.symphony.bdk.workflow.engine.executor.request;

import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.request.client.HttpClient;
import com.symphony.bdk.workflow.engine.executor.request.client.Response;
import com.symphony.bdk.workflow.swadl.v1.activity.request.ExecuteRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
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

    try {
      Response response =
          this.httpClient.execute(activity.getMethod(), activity.getUrl(), activity.getBody(),
              headersToString(activity.getHeaders()));

      execution.setOutputVariable(OUTPUT_STATUS_KEY, response.getCode());
      execution.setOutputVariable(OUTPUT_BODY_KEY, response.getContent());
    } catch (IOException ioException) {
      log.debug("This error happens when the request fails with IOException.", ioException);
      throw new RuntimeException(ioException.getMessage());
    }
  }

  private Map<String, String> headersToString(Map<String, Object> headers) {
    Map<String, String> result = new HashMap<>();
    for (Map.Entry<String, Object> entry : headers.entrySet()) {

      // We consider that opening/closing brackets are not allowed in headers values.
      // Otherwise, the list items should be added in the string one by one using map function
      String entryListAsString = List.of(entry.getValue()).toString();
      String entryWithoutBrackets = entryListAsString.replace("[", "").replace("]", "");
      String listToString = String.join(",", entryWithoutBrackets);

      result.put(entry.getKey(), String.join(",", listToString));
    }
    return result;
  }
}
