package com.symphony.bdk.workflow;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.options;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.symphony.bdk.workflow.custom.assertion.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

@WireMockTest
class ExecuteRequestIntegrationTest extends IntegrationTest {

  static Stream<Arguments> httpMethods() {
    return Stream.of(
        arguments(post(UrlPattern.ANY), "/request/execute-request-successful-POST.swadl.yaml",
            List.of("executePostRequest", "assertionScript")),
        arguments(put(UrlPattern.ANY), "/request/execute-request-successful-PUT.swadl.yaml",
            List.of("executePutRequest", "assertionScript")),
        arguments(delete(UrlPattern.ANY), "/request/execute-request-successful-DELETE.swadl.yaml",
            List.of("executeDeleteRequest", "assertionScript")),
        arguments(patch(UrlPattern.ANY), "/request/execute-request-successful-PATCH.swadl.yaml",
            List.of("executePatchRequest", "assertionScript")),
        arguments(options(UrlPattern.ANY), "/request/execute-request-successful-OPTIONS.swadl.yaml",
            List.of("executeOptionsRequest", "assertionScript"))
    );
  }

  @ParameterizedTest
  @MethodSource("httpMethods")
  void executeRequestSuccessful(MappingBuilder method, String swadlFile, List<String> activitiesToBeExecuted,
      WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream(swadlFile));

    this.putFirstActivityUrl(workflow, wmRuntimeInfo.getHttpBaseUrl() + "/api");

    stubFor(method.withHeader("keyOne", equalTo("valueOne"))
        .withHeader("keyTwo", equalTo("valueTwo,valueThree"))
        .withRequestBody(equalToJson("{\"key\":\"value\"}"))
        .willReturn(ok().withHeader("Content-Type", "application/json")
            .withBody("{\"name\": \"john\"}")));

    engine.deploy(workflow);

    engine.onEvent(messageReceived("/execute"));

    assertThat(workflow).isExecuted().executed(activitiesToBeExecuted.toArray(new String[0]));
  }

  @Test
  void executeRequestSuccessfulToDelete(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/request/execute-request-successful-HEAD.swadl.yaml"));

    putFirstActivityUrl(workflow, wmRuntimeInfo.getHttpBaseUrl() + "/api");

    stubFor(post(UrlPattern.ANY).withHeader("keyOne", equalTo("valueOne"))
        .withHeader("keyTwo", equalTo("valueTwo,valueThree"))
        .withRequestBody(equalToJson("{\"key\":\"value\"}"))
        .willReturn(ok().withHeader("Content-Type", "application/json")
            .withBody("{\"name\": \"john\"}")));

    engine.deploy(workflow);

    engine.onEvent(messageReceived("/execute"));

    assertThat(workflow).isExecuted().executed("executeHeadRequest", "assertionScript");
  }

  @Test
  void executeGetRequestSuccessful(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/request/execute-request-successful-GET.swadl.yaml"));

    putFirstActivityUrl(workflow, wmRuntimeInfo.getHttpBaseUrl() + "/api");

    stubFor(get(UrlPattern.ANY).withHeader("keyOne", equalTo("valueOne"))
        .withHeader("keyTwo", equalTo("valueTwo,valueThree"))
        .willReturn(ok().withHeader("Content-Type", "application/json").withBody("{\"name\": \"john\"}")));

    engine.deploy(workflow);

    engine.onEvent(messageReceived("/execute"));

    assertThat(workflow).isExecuted().executed("executeGetRequest", "assertionScript");
  }

  @Test
  void executeRequestException(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/request/execute-request-ioexception.swadl.yaml"));

    putFirstActivityUrl(workflow, wmRuntimeInfo.getHttpBaseUrl() + "/api");

    stubFor(post(UrlPattern.ANY).willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

    engine.deploy(workflow);

    engine.onEvent(messageReceived("/execute-failed"));

    assertThat(workflow).as("The workflow fails on runtime exception")
        .executed("executeGetRequest")
        .notExecuted("assertionScript");
  }

  @Test
  void executeRequestEncodeQueryParameters(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    final Workflow workflow = SwadlParser.fromYaml(
        getClass().getResourceAsStream("/request/execute-request-encode-query-parameters.swadl.yaml"));

    putFirstActivityUrl(workflow, wmRuntimeInfo.getHttpBaseUrl() + "/api?key1=v 1&key2=value@!$2&key3=value%3");

    stubFor(post("/api?key1=v+1&key2=value%40%21%242&key3=value%253")
        .willReturn(ok().withHeader("Content-Type", "application/json")
            .withBody("{\"message\": \"ok\"}")));

    engine.deploy(workflow);

    engine.onEvent(messageReceived("/execute"));

    assertThat(workflow).isExecuted().executed("executeRequestWithQueryParams", "assertionScript");
  }

  @Test
  void executeRequestNotEncodeQueryParameters(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    final Workflow workflow = SwadlParser.fromYaml(
        getClass().getResourceAsStream("/request/execute-request-not-encode-query-parameters.swadl.yaml"));

    putFirstActivityUrl(workflow, wmRuntimeInfo.getHttpBaseUrl() + "/api?key1=value%201&key2=value%402");

    stubFor(post(urlEqualTo("/api?key1=value%201&key2=value%402"))
        .willReturn(ok().withHeader("Content-Type", "application/json")
            .withBody("{\"message\": \"ok\"}")));

    engine.deploy(workflow);

    engine.onEvent(messageReceived("/execute"));

    assertThat(workflow).isExecuted().executed("executeRequestWithEncodedQueryParams", "assertionScript");
  }

  @Test
  void executeRequestEncodeQueryParametersNotExecuted(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    final Workflow workflow = SwadlParser.fromYaml(
        getClass().getResourceAsStream("/request/execute-request-not-encode-query-parameters-no-response.swadl.yaml"));

    putFirstActivityUrl(workflow, wmRuntimeInfo.getHttpBaseUrl() + "/api?key1=value%201&key2=value%402");

    stubFor(post("/api?key1=v+1&key2=value%40%21%242")
        .willReturn(ok().withHeader("Content-Type", "application/json")
            .withBody("{\"message\": \"ok\"}")));

    engine.deploy(workflow);

    engine.onEvent(messageReceived("/execute"));

    assertThat(workflow).isExecuted().executed("executeRequestWithEncodedQueryParams", "assertionScript");
  }

  private void putFirstActivityUrl(Workflow workflow, String url) {
    workflow.getFirstActivity()
        .get()
        .getActivity()
        .getVariableProperties()
        .put("url", url);
  }
}
