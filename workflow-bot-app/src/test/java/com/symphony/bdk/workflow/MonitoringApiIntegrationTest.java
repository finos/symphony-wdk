package com.symphony.bdk.workflow;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.api.v1.dto.TaskDefinitionView;
import com.symphony.bdk.workflow.api.v1.dto.TaskTypeEnum;
import com.symphony.bdk.workflow.monitoring.service.MonitoringService;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

class MonitoringApiIntegrationTest extends IntegrationTest {

  private static final String LIST_WORKFLOWS_PATH = "wdk/v1/workflows/";
  private static final String LIST_WORKFLOW_INSTANCES_PATH = "wdk/v1/workflows/%s/instances";
  private static final String LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH =
      "wdk/v1/workflows/%s/instances/%s/activities";
  private static final String LIST_WORKFLOW_DEFINITIONS_PATH = "/wdk/v1/workflows/%s/definitions";
  private static final String X_MONITORING_TOKEN_HEADER_KEY = "X-Monitoring-Token";
  private static final String X_MONITORING_TOKEN_HEADER_VALUE = "MONITORING_TOKEN_VALUE";
  private static final String INVALID_X_MONITORING_TOKEN_EXCEPTION_MESSAGE = "Request token is not valid";
  private static final String MISSING_X_MONITORING_TOKEN_HEADER_EXCEPTION_MESSAGE =
      "Request header X-Monitoring-Token is missing";
  private static final String BAD_WORKFLOW_INSTANCE_STATUS_EXCEPTION_MESSAGE =
      "Workflow instance status %s is not known. Allowed values [Completed, Pending]";

  @Autowired MonitoringService monitoringService;

  @ParameterizedTest
  @CsvSource(value = {LIST_WORKFLOWS_PATH, LIST_WORKFLOW_INSTANCES_PATH, LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH,
      LIST_WORKFLOW_DEFINITIONS_PATH})
  void badToken(String path) {
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, "BAD_TOKEN")
        .contentType(ContentType.JSON)
        .when()
        .get(path)
        .then()
        .assertThat()
        .statusCode(HttpStatus.UNAUTHORIZED.value())
        .body("message", equalTo(INVALID_X_MONITORING_TOKEN_EXCEPTION_MESSAGE));
  }

  @ParameterizedTest
  @CsvSource(value = {LIST_WORKFLOWS_PATH, LIST_WORKFLOW_INSTANCES_PATH, LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH,
      LIST_WORKFLOW_DEFINITIONS_PATH})
  void missingTokenHeader(String path) {
    given()
        .contentType(ContentType.JSON)
        .when()
        .get(path)
        .then()
        .assertThat()
        .statusCode(HttpStatus.UNAUTHORIZED.value())
        .body("message", equalTo(MISSING_X_MONITORING_TOKEN_HEADER_EXCEPTION_MESSAGE));
  }

  @Test
  void listAllWorkflows() throws Exception {
    final Workflow workflow1 =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-1.swadl.yaml"));
    final Workflow workflow2 =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-2.swadl.yaml"));
    final JsonPath expectedJson = new JsonPath(
        getClass().getResourceAsStream("/monitoring/expected/list-workflows-response-payload.json"));

    engine.deploy(workflow1);
    engine.deploy(workflow2);

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(LIST_WORKFLOWS_PATH)
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("", equalTo(expectedJson.getList("")));

    engine.undeploy("testingWorkflow1");
    engine.undeploy("testingWorkflow2");
  }

  @Test
  void listAllWorkflows_noWorkflowDeployed() {
    engine.undeployAll();

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(LIST_WORKFLOWS_PATH)
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("", empty());
  }

  @Test
  void listWorkflowInstances_noStatusFilter() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-3.swadl.yaml"));
    final V4Message message = message("Hello!");

    when(messageService.send(anyString(), any(Message.class))).thenReturn(message);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/testingWorkflow3"));

    // Wait for the workflow to get executed
    Thread.sleep(2000);

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCES_PATH, "testingWorkflow3"))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("[0].id", equalTo("testingWorkflow3"))
        .body("[0].version", equalTo(1))
        .body("[0].status", equalTo("COMPLETED"))
        .body("[0].instanceId", not(isEmptyString()))
        .body("[0].startDate", not(isEmptyString()))
        .body("[0].endDate", not(isEmptyString()));

    engine.undeploy("testingWorkflow3");
  }

  @Test
  void listWorkflowInstances_completedStatusFilter() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-6.swadl.yaml"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/testingWorkflow6"));

    // Wait for the workflow to get executed
    Thread.sleep(2000);

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCES_PATH + "?status=completed", "testingWorkflow6"))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("[0].id", equalTo("testingWorkflow6"))
        .body("[0].version", equalTo(1))
        .body("[0].status", equalTo("COMPLETED"))
        .body("[0].instanceId", not(isEmptyString()))
        .body("[0].startDate", not(isEmptyString()))
        .body("[0].endDate", not(isEmptyString()));

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCES_PATH + "?status=pending", "testingWorkflow6"))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("", empty());

    engine.undeploy("testingWorkflow6");
  }

  @Test
  void listWorkflowInstances_pendingStatusFilter() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-7.swadl.yaml"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/testingWorkflow7"));

    // Wait for the workflow to get executed
    Thread.sleep(2000);

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCES_PATH + "?status=pending", "testingWorkflow7"))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("[0].id", equalTo("testingWorkflow7"))
        .body("[0].version", equalTo(1))
        .body("[0].status", equalTo("PENDING"))
        .body("[0].instanceId", not(isEmptyString()))
        .body("[0].startDate", not(isEmptyString()))
        .body("[0].endDate", not(isEmptyString()));

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCES_PATH + "?status=completed", "testingWorkflow7"))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("", empty());

    engine.undeploy("testingWorkflow7");
  }

  @Test
  void listWorkflowInstances_unknownStatusFilter() {
    final String path = LIST_WORKFLOW_INSTANCES_PATH + "?status=unknownStatus";
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(path, "testingWorkflow7"))
        .then()
        .assertThat()
        .statusCode(HttpStatus.NOT_FOUND.value())
        .body("message", equalTo(String.format(BAD_WORKFLOW_INSTANCE_STATUS_EXCEPTION_MESSAGE, "unknownStatus")));
  }

  @Test
  void listWorkflowInstances_unknownWorkflow() {
    engine.undeployAll();

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCES_PATH, "testingWorkflow1"))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("", empty());
  }

  @Test
  void listInstanceActivities() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-4.swadl.yaml"));

    final V4Message message = message("Hello!");

    when(messageService.send(anyString(), any(Message.class))).thenReturn(message);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/testingWorkflow4"));

    // Wait for the workflow to get executed
    Thread.sleep(2000);

    String processDefinition = this.getOneProcessInstanceId("testingWorkflow4");

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH, "testingWorkflow4", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("activities[0].workflowId", equalTo("testingWorkflow4"))
        .body("activities[0].instanceId", not(empty()))
        .body("activities[0].activityId", equalTo("testingWorkflow4SendMsg1"))
        .body("activities[0].type", equalTo("SEND_MESSAGE_ACTIVITY"))
        .body("activities[0].startDate", not(empty()))
        .body("activities[0].endDate", not(empty()))
        .body("activities[0].duration", not(empty()))
        .body("activities[0].outputs.message", not(empty()))
        .body("activities[0].outputs.msgId", not(empty()))

        .body("activities[1].workflowId", equalTo("testingWorkflow4"))
        .body("activities[1].instanceId", not(empty()))
        .body("activities[1].activityId", equalTo("testingWorkflow4SendMsg2"))
        .body("activities[1].type", equalTo("SEND_MESSAGE_ACTIVITY"))
        .body("activities[1].startDate", not(empty()))
        .body("activities[1].endDate", not(empty()))
        .body("activities[1].duration", not(empty()))
        .body("activities[1].outputs.message", not(empty()))
        .body("activities[1].outputs.msgId", not(empty()))

        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    engine.undeploy("testingWorkflow4");
  }

  @Test
  @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
  void listInstanceActivities_startedBeforeFilter() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-8.swadl.yaml"));

    engine.deploy(workflow);

    Instant beforeFirstSlashInstant = Instant.now();

    engine.onEvent(messageReceived("/testingWorkflow8"));

    // Wait for the first activity to get executed
    Thread.sleep(2000);

    Instant afterFirstSlashInstant = Instant.now();

    engine.onEvent(messageReceived("/continueTestingWorkflow8"));

    // Wait for the second activity to get executed
    Thread.sleep(2000);

    Instant afterSecondSlashInstant = Instant.now();

    String processDefinition = this.getOneProcessInstanceId("testingWorkflow8");

    // No activity started yet
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + "?started_before=" + beforeFirstSlashInstant,
            "testingWorkflow8", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("activities", hasSize(0))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    // Only the first activity has started
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + "?started_before=" + afterFirstSlashInstant,
            "testingWorkflow8", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("activities", hasSize(1))
        .body("activities[0].workflowId", equalTo("testingWorkflow8"))
        .body("activities[0].instanceId", not(empty()))
        .body("activities[0].activityId", equalTo("script1TestingWorkflow8"))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    // Both workflow's activities have started
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + "?started_before=" + afterSecondSlashInstant,
            "testingWorkflow8", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("activities", hasSize(2))
        .body("activities[0].workflowId", equalTo("testingWorkflow8"))
        .body("activities[0].activityId", equalTo("script1TestingWorkflow8"))
        .body("activities[0].instanceId", not(empty()))
        .body("activities[1].workflowId", equalTo("testingWorkflow8"))
        .body("activities[1].instanceId", not(empty()))
        .body("activities[1].activityId", equalTo("script2TestingWorkflow8"))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    engine.undeploy("testingWorkflow8");
  }

  @Test
  @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
  void listInstanceActivities_startedAfterFilter() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-9.swadl.yaml"));

    engine.deploy(workflow);

    Instant beforeFirstSlashInstant = Instant.now();

    engine.onEvent(messageReceived("/testingWorkflow9"));

    // Wait for the first activity to get executed
    Thread.sleep(2000);

    Instant afterFirstSlashInstant = Instant.now();

    engine.onEvent(messageReceived("/continueTestingWorkflow9"));

    // Wait for the second activity to get executed
    Thread.sleep(2000);

    Instant afterSecondSlashInstant = Instant.now();

    String processDefinition = this.getOneProcessInstanceId("testingWorkflow9");

    // Both workflow's activities have started
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + "?started_after=" + beforeFirstSlashInstant,
            "testingWorkflow9", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("activities", hasSize(2))
        .body("activities[0].workflowId", equalTo("testingWorkflow9"))
        .body("activities[0].activityId", equalTo("script1TestingWorkflow9"))
        .body("activities[0].instanceId", not(empty()))
        .body("activities[1].workflowId", equalTo("testingWorkflow9"))
        .body("activities[1].instanceId", not(empty()))
        .body("activities[1].activityId", equalTo("script2TestingWorkflow9"))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    // Only the second activity has started
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + "?started_after=" + afterFirstSlashInstant,
            "testingWorkflow9", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("activities", hasSize(1))
        .body("activities[0].workflowId", equalTo("testingWorkflow9"))
        .body("activities[0].instanceId", not(empty()))
        .body("activities[0].activityId", equalTo("script2TestingWorkflow9"))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    // No activity started
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + "?started_after=" + afterSecondSlashInstant,
            "testingWorkflow9", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("activities", hasSize(0))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    engine.undeploy("testingWorkflow9");
  }

  @Test
  @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
  void listInstanceActivities_finishedBeforeFilter() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-10.swadl.yaml"));

    engine.deploy(workflow);

    Instant beforeFirstSlashInstant = Instant.now();

    engine.onEvent(messageReceived("/testingWorkflow10"));

    // Wait for the first activity to get executed
    Thread.sleep(2000);

    Instant afterFirstSlashInstant = Instant.now();

    engine.onEvent(messageReceived("/continueTestingWorkflow10"));

    // Wait for the second activity to get executed
    Thread.sleep(2000);

    Instant afterSecondSlashInstant = Instant.now();

    String processDefinition = this.getOneProcessInstanceId("testingWorkflow10");

    // No activity finished yet
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + "?finished_before=" + beforeFirstSlashInstant,
            "testingWorkflow10", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("activities", hasSize(0))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    // Only the first activity has finished
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + "?finished_before=" + afterFirstSlashInstant,
            "testingWorkflow10", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("activities", hasSize(1))
        .body("activities[0].workflowId", equalTo("testingWorkflow10"))
        .body("activities[0].instanceId", not(empty()))
        .body("activities[0].activityId", equalTo("script1TestingWorkflow10"))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    // Both workflow's activities have finished
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + "?finished_before=" + afterSecondSlashInstant,
            "testingWorkflow10", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("activities", hasSize(2))
        .body("activities[0].workflowId", equalTo("testingWorkflow10"))
        .body("activities[0].activityId", equalTo("script1TestingWorkflow10"))
        .body("activities[0].instanceId", not(empty()))
        .body("activities[1].workflowId", equalTo("testingWorkflow10"))
        .body("activities[1].instanceId", not(empty()))
        .body("activities[1].activityId", equalTo("script2TestingWorkflow10"))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    engine.undeploy("testingWorkflow10");
  }

  @Test
  @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
  void listInstanceActivities_finishedAfterFilter() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-11.swadl.yaml"));

    engine.deploy(workflow);

    Instant beforeFirstSlashInstant = Instant.now();

    engine.onEvent(messageReceived("/testingWorkflow11"));

    // Wait for the first activity to get executed
    Thread.sleep(2000);

    Instant afterFirstSlashInstant = Instant.now();

    engine.onEvent(messageReceived("/continueTestingWorkflow11"));

    // Wait for the second activity to get executed
    Thread.sleep(2000);

    Instant afterSecondSlashInstant = Instant.now();

    String processDefinition = this.getOneProcessInstanceId("testingWorkflow11");

    // Both workflow's activities have finished
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + "?finished_after=" + beforeFirstSlashInstant,
            "testingWorkflow11", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("activities", hasSize(2))
        .body("activities[0].workflowId", equalTo("testingWorkflow11"))
        .body("activities[0].activityId", equalTo("script1TestingWorkflow11"))
        .body("activities[0].instanceId", not(empty()))
        .body("activities[1].workflowId", equalTo("testingWorkflow11"))
        .body("activities[1].instanceId", not(empty()))
        .body("activities[1].activityId", equalTo("script2TestingWorkflow11"))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    // Only the second activity has finished
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + "?finished_after=" + afterFirstSlashInstant,
            "testingWorkflow11", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("activities", hasSize(1))
        .body("activities[0].workflowId", equalTo("testingWorkflow11"))
        .body("activities[0].instanceId", not(empty()))
        .body("activities[0].activityId", equalTo("script2TestingWorkflow11"))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    // No activity finished yet
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + "?finished_after=" + afterSecondSlashInstant,
            "testingWorkflow11", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("activities", hasSize(0))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    engine.undeploy("testingWorkflow11");
  }

  @Test
  void listInstanceActivities_unknownWorkflowId_unknownInstanceId() {
    final String unknownWorkflowId = "unknownWorkflowId";
    final String unknownInstanceId = "unknownInstanceId";
    final String expectedErrorMsg =
        String.format("Either no workflow deployed with id %s, or %s is not an instance of it", unknownWorkflowId,
            unknownInstanceId);

    engine.undeploy(unknownWorkflowId);

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH, unknownWorkflowId, unknownInstanceId))
        .then()
        .assertThat()
        .statusCode(HttpStatus.NOT_FOUND.value())
        .body("message", equalTo(expectedErrorMsg));
  }

  @Test
  void listWorkflowActivitiesDefinitions() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-5.swadl.yaml"));

    engine.deploy(workflow);

    // Wait for the workflow to get executed
    Thread.sleep(2000);

    Response response = given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_DEFINITIONS_PATH, "testingWorkflow5"))
        .thenReturn();

    // actual flow nodes
    ObjectMapper objectMapper = new ObjectMapper();
    List<TaskDefinitionView> taskDefinitionViews = new ArrayList<>();
    List<Object> flowNodes = response.body().jsonPath().getList("flowNodes");

    flowNodes.forEach(flowNode -> {
      try {
        taskDefinitionViews.add(
            objectMapper.readValue(objectMapper.writeValueAsString(flowNode), TaskDefinitionView.class));
      } catch (JsonProcessingException e) {
        e.printStackTrace();
        fail("Unexpected error when converting api response to TaskDefinitionView class");
      }
    });

    // expected flow nodes
    TaskDefinitionView expectedSendMessageActivity1 = TaskDefinitionView.builder()
        .nodeId("testingWorkflow5SendMsg1")
        .type(TaskTypeEnum.SEND_MESSAGE_ACTIVITY)
        .parents(Collections.singletonList("message-received_/testingWorkflow5"))
        .children(Collections.singletonList("testingWorkflow5SendMsg2"))
        .build();

    TaskDefinitionView expectedSendMessageActivity2 = TaskDefinitionView.builder()
        .nodeId("testingWorkflow5SendMsg2")
        .type(TaskTypeEnum.SEND_MESSAGE_ACTIVITY)
        .parents(Collections.singletonList("testingWorkflow5SendMsg1"))
        .children(Collections.emptyList())
        .build();

    TaskDefinitionView expectedMessageReceivedEventTask = TaskDefinitionView.builder()
        .nodeId("message-received_/testingWorkflow5")
        .type(TaskTypeEnum.MESSAGE_RECEIVED_EVENT)
        .parents(Collections.emptyList())
        .children(Collections.singletonList("testingWorkflow5SendMsg1"))
        .build();

    List<TaskDefinitionView> expectedTaskDefinitions =
        Arrays.asList(expectedSendMessageActivity1, expectedSendMessageActivity2, expectedMessageReceivedEventTask);

    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(response.body().jsonPath().getString("workflowId")).isEqualTo("testingWorkflow5");
    assertThat(response.body().jsonPath().getList("variables")).isEmpty();

    assertThat(taskDefinitionViews)
        .hasSameSizeAs(expectedTaskDefinitions)
        .hasSameElementsAs(expectedTaskDefinitions);

    engine.undeploy("testingWorkflow5");
  }

  @Test
  void listWorkflowActivitiesDefinitions_unknownWorkflowId() {
    final String unknownWorkflowId = "unknownWorkflowId";
    final String expectedErrorMsg = String.format("No workflow deployed with id '%s' is found", unknownWorkflowId);

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_DEFINITIONS_PATH, unknownWorkflowId))
        .then()
        .assertThat()
        .statusCode(HttpStatus.NOT_FOUND.value())
        .body("message", equalTo(expectedErrorMsg));
  }

  private String getOneProcessInstanceId(String workflowId) {
    Optional<String> processDefinition = historyService.createHistoricProcessInstanceQuery()
        .processDefinitionKey(workflowId)
        .list()
        .stream()
        .map(HistoricProcessInstance::getId)
        .findFirst();

    if (processDefinition.isEmpty()) {
      fail("At least one process definition should have been found.");
    }

    return processDefinition.get();
  }
}

