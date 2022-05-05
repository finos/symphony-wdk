package com.symphony.bdk.workflow.engine.executor.request;

import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.request.client.HttpClient;
import com.symphony.bdk.workflow.engine.executor.request.client.Response;
import com.symphony.bdk.workflow.swadl.v1.activity.request.ExecuteRequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ExecuteRequestExecutor implements ActivityExecutor<ExecuteRequest> {

  private static final String OUTPUT_STATUS_KEY = "status";
  private static final String OUTPUT_BODY_KEY = "body";

  private final HttpClient httpClient;

  public ExecuteRequestExecutor(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Override
  public void execute(ActivityExecutorContext<ExecuteRequest> execution) throws IOException {
    ExecuteRequest activity = execution.getActivity();
    activity.setUrl(this.encodeQueryParameters(activity.getUrl()));

    log.info("Executing request {} {}", activity.getMethod(), activity.getUrl());

    Response response =
        this.httpClient.execute(activity.getMethod(), activity.getUrl(), activity.getBody(),
            headersToString(activity.getHeaders()));

    log.info("Received response {}", response.getCode());

    Map<String, Object> outputs = new HashMap<>();
    outputs.put(OUTPUT_STATUS_KEY, response.getCode());
    outputs.put(OUTPUT_BODY_KEY, response.getContent());
    execution.setOutputVariables(outputs);
  }

  private String encodeQueryParameters(String fullUrl) {
    // splits by the first occurrence of "?" to get the query params part
    String[] splitUrl = fullUrl.split("\\?", 2);
    String encodedUrl = splitUrl[0];

    MultiValueMap<String, String> queryParamsMap =
        UriComponentsBuilder.fromUriString(fullUrl).build().getQueryParams();

    if (!queryParamsMap.isEmpty()) {
      log.info("Encoding query parameters with Standard UTF-8 format");

      String rawQuery = queryParamsMap
          .keySet()
          .stream()
          .map(key ->
              key + "=" + URLEncoder.encode(queryParamsMap.get(key).get(0), StandardCharsets.UTF_8))
          .collect(Collectors.joining("&"));

      encodedUrl += "?" + rawQuery;
    }

    return encodedUrl;
  }

  private Map<String, String> headersToString(Map<String, Object> headers) {
    Map<String, String> result = new HashMap<>();
    for (Map.Entry<String, Object> entry : headers.entrySet()) {
      String joinedHeaders = String.join(",", toList(entry.getValue()));
      result.put(entry.getKey(), joinedHeaders);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private List<String> toList(Object object) {
    if (object instanceof List) {
      return new ArrayList<>(((List<String>) object));
    } else {
      return List.of(object.toString());
    }
  }
}
