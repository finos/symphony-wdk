package com.symphony.bdk.workflow.engine.executor.request;

import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class ExecuteRequestUtils {

  ExecuteRequestUtils() {}

  public static String encodeQueryParameters(String fullUrl) {
    MultiValueMap<String, String> queryParamsMap =
        UriComponentsBuilder.fromUriString(fullUrl).build().getQueryParams();

    UriComponentsBuilder uriComponentsBuilder =
        UriComponentsBuilder.fromUriString(fullUrl);

    queryParamsMap
        .keySet()
        .forEach(
            key -> {
              List<String> encodedValues =
                  queryParamsMap.get(key)
                      .stream()
                      .map(ExecuteRequestUtils::encode)
                      .collect(Collectors.toList());
              uriComponentsBuilder.replaceQueryParam(key, encodedValues);
            });

    return uriComponentsBuilder.build().toUriString();
  }

  private static String encode(String value) {
    try {
      return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      return value;
    }
  }
}
