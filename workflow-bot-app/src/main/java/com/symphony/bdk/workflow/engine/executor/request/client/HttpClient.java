package com.symphony.bdk.workflow.engine.executor.request.client;

import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.fluent.Form;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class HttpClient {

  public Response execute(String method, String url, Map<String, Object> body, Map<String, String> headers)
      throws IOException {
    HttpResponse httpResponse;
    switch (method) {
      case "GET":
        httpResponse = this.get(url);
        break;
      case "POST":
        httpResponse = this.post(url, body, headers);
        break;
      case "PUT":
        httpResponse = this.put(url, body, headers);
        break;
      case "DELETE":
        httpResponse = this.delete(url, body, headers);
        break;
      case "PATCH":
        httpResponse = this.patch(url, body, headers);
        break;
      case "HEAD":
        httpResponse = this.head(url, body, headers);
        break;
      case "OPTIONS":
        httpResponse = this.options(url, body, headers);
        break;
      default:
        throw new RuntimeException("Method is not supported");
    }

    ClassicHttpResponse classicHttpResponse = (ClassicHttpResponse) httpResponse;
    int responseCode = httpResponse.getCode();
    String responseContent = IOUtils.toString(classicHttpResponse.getEntity().getContent(), StandardCharsets.UTF_8);
    return new Response(responseCode, responseContent);
  }

  private HttpResponse get(String url) throws IOException {
    return Request.get(url).execute().returnResponse();
  }

  private HttpResponse post(String url, Map<String, Object> body, Map<String, String> headers) throws IOException {
    Request request = Request.post(url);
    return this.executeWithBody(request, body, headers);
  }

  private HttpResponse put(String url, Map<String, Object> body, Map<String, String> headers) throws IOException {
    Request request = Request.put(url);
    return this.executeWithBody(request, body, headers);
  }

  private HttpResponse delete(String url, Map<String, Object> body, Map<String, String> headers) throws IOException {
    Request request = Request.delete(url);
    return this.executeWithBody(request, body, headers);
  }

  private HttpResponse patch(String url, Map<String, Object> body, Map<String, String> headers) throws IOException {
    Request request = Request.patch(url);
    return this.executeWithBody(request, body, headers);
  }

  private HttpResponse head(String url, Map<String, Object> body, Map<String, String> headers) throws IOException {
    Request request = Request.head(url);
    return this.executeWithBody(request, body, headers);
  }

  private HttpResponse options(String url, Map<String, Object> body, Map<String, String> headers) throws IOException {
    Request request = Request.options(url);
    return this.executeWithBody(request, body, headers);
  }

  private HttpResponse executeWithBody(Request request, Map<String, Object> body, Map<String, String> headers)
      throws IOException {
    // set body
    Form form = Form.form();
    body.forEach((key, value) -> form.add(key, value.toString()));
    request.bodyForm(form.build());

    // set headers
    headers.forEach(request::addHeader);

    return request.execute().returnResponse();
  }

}
