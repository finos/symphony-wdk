package com.symphony.bdk.workflow.engine.executor.request.client;

import lombok.Generated;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Generated
@Component
public class HttpClient {

  public Response execute(String method, String url, Object body, Map<String, String> headers)
      throws IOException {
    Request request = Request.create(method, url);
    HttpResponse httpResponse = this.executeWithBody(request, body, headers);

    ClassicHttpResponse classicHttpResponse = (ClassicHttpResponse) httpResponse;
    int responseCode = httpResponse.getCode();

    if (classicHttpResponse.getEntity() != null && classicHttpResponse.getEntity().getContent() != null) {
      return new Response(responseCode,
          IOUtils.toString(classicHttpResponse.getEntity().getContent(), StandardCharsets.UTF_8));
    } else {
      return new Response(responseCode, "");
    }
  }

  @SuppressWarnings("unchecked")
  private HttpResponse executeWithBody(Request request, Object body, Map<String, String> headers)
      throws IOException {

    // set body
    String contentType = headers.get(HttpHeaders.CONTENT_TYPE);
    if (body != null && ContentType.MULTIPART_FORM_DATA.getMimeType().equals(contentType)) {

      final MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder
          .create();

      Map<String, Object> bodyAsMap = (LinkedHashMap<String, Object>) body;
      bodyAsMap.forEach((key, value) -> multipartEntityBuilder.addTextBody(key, value.toString()));

      request.body(multipartEntityBuilder.build());

      // The content type with boundary is provided in the entity, otherwise it is overridden
      headers.remove(HttpHeaders.CONTENT_TYPE);

    } else if (body != null && StringUtils.isNotEmpty(contentType)) {
      request.bodyString(body.toString(), ContentType.parse(contentType));
    } else if (body != null) { // if no content type is provided, we set application/json by default
      request.bodyString(body.toString(), ContentType.APPLICATION_JSON);
    }

    // set headers
    headers.forEach(request::addHeader);

    return request.execute().returnResponse();
  }

}
