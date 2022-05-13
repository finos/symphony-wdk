package com.symphony.bdk.workflow.engine.executor.request;

import lombok.experimental.UtilityClass;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class ExecuteRequestUtils {

  public static String encodeQueryParameters(String fullUrl) {
    UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(fullUrl);
    MultiValueMap<String, String> queryParamsMap = uriComponentsBuilder.build().getQueryParams();

    UriComponentsBuilder clone = uriComponentsBuilder.cloneBuilder();
    queryParamsMap
        .keySet()
        .forEach(key -> {
          List<String> encodedValues =
              queryParamsMap.get(key)
                  .stream()
                  .map(ExecuteRequestUtils::encode)
                  .collect(Collectors.toList());
          clone.replaceQueryParam(key, encodedValues);
        });

    return clone.build().toUriString();
  }

  private static String encode(String value) {
    try {
      return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      return value;
    }
  }
}
