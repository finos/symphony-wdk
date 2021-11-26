package com.symphony.bdk.workflow;

import static com.symphony.bdk.workflow.custom.assertion.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.symphony.bdk.workflow.engine.executor.request.client.Response;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

class ExecuteRequestIntegrationTest extends IntegrationTest {

  private static final String OUTPUTS_STATUS_KEY = "%s.outputs.status";
  private static final String OUTPUTS_BODY_KEY = "%s.outputs.body";


  static Stream<Arguments> httpMethods() {
    return Stream.of(
        arguments("POST", "/request/execute-request-successful-POST.swadl.yaml", "executePostRequest"),
        arguments("PUT", "/request/execute-request-successful-PUT.swadl.yaml", "executePutRequest"),
        arguments("DELETE", "/request/execute-request-successful-DELETE.swadl.yaml", "executeDeleteRequest"),
        arguments("PATCH", "/request/execute-request-successful-PATCH.swadl.yaml", "executePatchRequest"),
        arguments("HEAD", "/request/execute-request-successful-HEAD.swadl.yaml", "executeHeadRequest"),
        arguments("OPTIONS", "/request/execute-request-successful-OPTIONS.swadl.yaml", "executeOptionsRequest")
    );
  }

  @ParameterizedTest
  @MethodSource("httpMethods")
  void executeRequestSuccessful(String method, String swadlFile, String workflowId) throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream(swadlFile));

    final Map<String, String> header = Map.of("keyOne", "valueOne", "keyTwo", "valueTwo,valueThree");
    final Map<String, Object> body = Map.of("args", Map.of("key", "value"));
    final String url = "https://url.com?isMocked=true";
    final String expectedResponse =
        "{\"name\": \"john\",\n \"age\": \"22\",\n"
            + "\"contact\": {\n\"phone\": \"0123456\",\n\"email\": \"john@symphony.com\"}";

    final Response mockedResponse = new Response(200, expectedResponse);

//    when(httpClient.execute(method, url, body, header)).thenReturn(mockedResponse);

    engine.deploy(workflow);

    engine.onEvent(messageReceived("/execute"));

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUTS_STATUS_KEY, workflowId), 200)
        .hasOutput(String.format(OUTPUTS_BODY_KEY, workflowId), expectedResponse);
  }

  @Test
  void executeGetRequestSuccessful() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/request/execute-request-successful-GET.swadl.yaml"));

    final Map<String, String> header = Map.of("keyOne", "valueOne", "keyTwo", "valueTwo,valueThree");
    final String url = "https://url.com?isMocked=true";
    final String expectedResponse =
        "{\"name\": \"john\",\n \"age\": \"22\","
            + "\n\"contact\": {\n\"phone\": \"0123456\",\n\"email\": \"john@symphony.com\"}";

    final Response mockedResponse = new Response(200, expectedResponse);

//    when(httpClient.execute("GET", url, null, header)).thenReturn(mockedResponse);

    engine.deploy(workflow);

    engine.onEvent(messageReceived("/execute"));

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUTS_STATUS_KEY, "executeGetRequest"), 200)
        .hasOutput(String.format(OUTPUTS_BODY_KEY, "executeGetRequest"), expectedResponse);
  }

  static Stream<Arguments> exceptionMessages() {
    return Stream.of(
        arguments("{\"message\": \"ApiException response body\"}"),
        arguments("ApiException response body")
    );
  }

  @ParameterizedTest
  @MethodSource("exceptionMessages")
  void executeRequestFailed(String exceptionMessage) throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/request/execute-request-failed.swadl.yaml"));

    final Map<String, String> header = Map.of("headerKey", "headerValue");
    final Map<String, Object> body = Map.of("args", Map.of("key", "value"));
    final String url = "https://url.com?isMocked=true";

//    when(httpClient.execute("POST", url, body, header)).thenReturn(new Response(400, exceptionMessage));

    engine.deploy(workflow);

    engine.onEvent(messageReceived("/execute-failed"));

    assertThat(workflow).isExecuted()
        .hasOutput(String.format(OUTPUTS_STATUS_KEY, "executeGetRequest"), 400)
        .hasOutput(String.format(OUTPUTS_BODY_KEY, "executeGetRequest"), exceptionMessage);
  }

  @Test
  void executeRequestIoException() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/request/execute-request-ioexception.swadl.yaml"));

    final Map<String, String> header = Map.of("headerKey", "headerValue");
    final Map<String, Object> body = Map.of("args", Map.of("key", "value"));
    final String url = "https://url.com?isMocked=true";
    final String exceptionMessage = "IOException message";

//    when(httpClient.execute("POST", url, body, header)).thenThrow(new IOException(exceptionMessage));

    engine.deploy(workflow);

    engine.onEvent(messageReceived("/execute-failed"));

    assertThat(workflow).as("The workflow fails on runtime exception")
        .executed("executeGetRequest")
        .notExecuted("assertionScript");
  }
}
