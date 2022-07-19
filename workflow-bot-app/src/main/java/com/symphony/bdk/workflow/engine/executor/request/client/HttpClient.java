package com.symphony.bdk.workflow.engine.executor.request.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Generated;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
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

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public Response execute(String method, String url, Object body, Map<String, String> headers)
      throws IOException {
    Request request = Request.create(method, url);
    HttpResponse httpResponse = this.executeWithBody(request, body, headers);

    ClassicHttpResponse classicHttpResponse = (ClassicHttpResponse) httpResponse;
    int responseCode = httpResponse.getCode();

    if (classicHttpResponse.getEntity() != null && classicHttpResponse.getEntity().getContent() != null) {
      return this.handleResponse(responseCode,
          IOUtils.toString(classicHttpResponse.getEntity().getContent(), StandardCharsets.UTF_8),
          classicHttpResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE));
    } else {
      return new Response(responseCode, "");
    }
  }

  private Response handleResponse(int statusCode, String content, Header contentType) {
    Object data = content;
    if (isJsonContentOrNull(contentType)) {
      try {
        data = OBJECT_MAPPER.readValue(content, Map.class);
      } catch (JsonProcessingException jsonProcessingException) {
        //content is already assigned to data
      }
    }

    return new Response(statusCode, data);
  }

  private boolean isJsonContentOrNull(Header contentType) {
    return contentType == null || contentType.getValue().contains(ContentType.APPLICATION_JSON.getMimeType());
  }

  @SuppressWarnings("unchecked")
  private HttpResponse executeWithBody(Request request, Object body, Map<String, String> headers)
      throws IOException {

    // set body
    String headerContentType = headers.get(HttpHeaders.CONTENT_TYPE);
    if (body != null && ContentType.MULTIPART_FORM_DATA.getMimeType().equals(headerContentType)) {

      final MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder
          .create();

      Map<String, Object> bodyAsMap = (LinkedHashMap<String, Object>) body;
      ContentType textBodyContentType = ContentType.create("text/plain", StandardCharsets.UTF_8);
      bodyAsMap.forEach((key, value) -> multipartEntityBuilder.addTextBody(key, value.toString(), textBodyContentType));

      request.body(multipartEntityBuilder.build());

      // The content type with boundary is provided in the entity, otherwise it is overridden
      headers.remove(HttpHeaders.CONTENT_TYPE);

    } else if (body != null && StringUtils.isNotEmpty(headerContentType)) {
      if (headerContentType.equals(ContentType.APPLICATION_JSON.getMimeType()) && !(body instanceof String)) {
        request.bodyString(OBJECT_MAPPER.writeValueAsString(body), ContentType.APPLICATION_JSON);
      } else {
        request.bodyString(body.toString(), ContentType.parse(headerContentType));
      }

    } else if (body != null) { // if no content type is provided, we set application/json by default
      if (body instanceof String) {
        request.bodyString(body.toString(), ContentType.APPLICATION_JSON);
      } else {
        request.bodyString(OBJECT_MAPPER.writeValueAsString(body), ContentType.APPLICATION_JSON);
      }
    }

    // set headers
    headers.forEach(request::addHeader);

    return request.execute().returnResponse();
  }

}
