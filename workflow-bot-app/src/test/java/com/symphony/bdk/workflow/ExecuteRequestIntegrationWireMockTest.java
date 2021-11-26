package com.symphony.bdk.workflow;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.symphony.bdk.workflow.custom.assertion.Assertions.assertThat;

import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@WireMockTest
class ExecuteRequestIntegrationWireMockTest extends IntegrationTest {

  @Test
  void testPostJson(WireMockRuntimeInfo wmRuntimeInfo) throws IOException, ProcessingException, InterruptedException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/request/execute-request-JSON-POST.swadl.yaml"));

    workflow.getFirstActivity().get().getActivity().getVariableProperties()
        .put("url", wmRuntimeInfo.getHttpBaseUrl() + "/api");

    stubFor(post(UrlPattern.ANY).withRequestBody(equalToJson("{\"key\":\"value\"}")).willReturn(
        ok().withHeader("Content-Type", "application/json")
            .withBody("{\"name\": \"john\"}")));

//    Thread.sleep(100000);

    engine.deploy(workflow);

    engine.onEvent(messageReceived("/execute"));

    assertThat(workflow).isExecuted();
  }

}
