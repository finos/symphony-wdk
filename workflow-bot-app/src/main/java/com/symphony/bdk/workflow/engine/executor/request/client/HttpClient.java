package com.symphony.bdk.workflow.engine.executor.request.client;

import lombok.Generated;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.fluent.Form;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Generated
@SuppressWarnings("unchecked")
@Component
public class HttpClient {

  private static final String CONTENT_TYPE = "Content-Type";

  public Response execute(String method, String url, Object body, Map<String, String> headers)
      throws IOException {
    HttpResponse httpResponse;
    Request request;
    switch (method) {
      case "GET":
        request = Request.get(url);
        httpResponse = this.executeWithoutBody(request);
        break;
      case "POST":
        request = Request.post(url);
        httpResponse = this.executeWithBody(request, body, headers);
        break;
      case "PUT":
        request = Request.put(url);
        httpResponse = this.executeWithBody(request, body, headers);
        break;
      case "DELETE":
        request = Request.delete(url);
        httpResponse = this.executeWithBody(request, body, headers);
        break;
      case "PATCH":
        request = Request.patch(url);
        httpResponse = this.executeWithBody(request, body, headers);
        break;
      case "HEAD":
        request = Request.head(url);
        httpResponse = this.executeWithBody(request, body, headers);
        break;
      case "OPTIONS":
        request = Request.options(url);
        httpResponse = this.executeWithBody(request, body, headers);
        break;
      default:
        throw new RuntimeException("Method is not supported");
    }

    ClassicHttpResponse classicHttpResponse = (ClassicHttpResponse) httpResponse;
    int responseCode = httpResponse.getCode();

    if (classicHttpResponse.getEntity() != null && classicHttpResponse.getEntity().getContent() != null) {
      return new Response(responseCode,
          IOUtils.toString(classicHttpResponse.getEntity().getContent(), StandardCharsets.UTF_8));
    } else {
      return new Response(responseCode, "");
    }
  }

  private HttpResponse executeWithoutBody(Request request) throws IOException {
    return request.execute().returnResponse();
  }

  private HttpResponse executeWithBody(Request request, Object body, Map<String, String> headers)
      throws IOException {

    // set body
    if (ContentType.APPLICATION_JSON.getMimeType().equals(headers.get(CONTENT_TYPE))) {
      request.bodyString(body.toString(), ContentType.APPLICATION_JSON);
    } else if (ContentType.MULTIPART_FORM_DATA.getMimeType().equals(headers.get(CONTENT_TYPE))) {
      Form form = Form.form();
      Map<String, Object> bodyAsMap = (HashMap<String, Object>) body;
      bodyAsMap.forEach((key, value) -> form.add(key, value.toString()));
      request.bodyForm(form.build());
    }

    // set headers
    headers.forEach(request::addHeader);

    return request.execute().returnResponse();
  }

}
