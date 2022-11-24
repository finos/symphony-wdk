package com.symphony.bdk.workflow;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

class CustomActivityIntegrationTest extends IntegrationTest {

  @Test
  void customActivity() throws Exception {
    Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream("/custom-activity.swadl.yaml"));
    engine.deploy(workflow);

    engine.onEvent(messageReceived("/execute"));

    // com.symphony.bdk.workflow.DoSomethingExecutor is just sending a message
    verify(messageService, timeout(5000)).send("123", "abc");

    Response response = given()
        .header("X-Monitoring-Token", "MONITORING_TOKEN_VALUE")
        .contentType(ContentType.JSON)
        .when()
        .get(String.format("/wdk/v1/workflows/%s/definitions", "custom-activity"))
        .thenReturn();

    // actual flow nodes
    List<Object> flowNodes = response.body().jsonPath().getList("flowNodes");
    assertThat(((LinkedHashMap) flowNodes.get(0)).get("type")).isEqualTo("DO_SOMETHING");
    assertThat(((LinkedHashMap) flowNodes.get(0)).get("group")).isEqualTo("ACTIVITY");
  }

}
