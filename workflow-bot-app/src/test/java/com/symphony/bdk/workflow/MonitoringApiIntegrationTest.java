package com.symphony.bdk.workflow;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.api.v1.dto.NodeView;
import com.symphony.bdk.workflow.engine.WorkflowNodeTypeHelper;
import com.symphony.bdk.workflow.management.repository.VersionedWorkflowRepository;
import com.symphony.bdk.workflow.management.repository.domain.VersionedWorkflow;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.junit.jupiter.api.Disabled;
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

import static com.symphony.bdk.workflow.api.v1.dto.NodeView.ChildView;
import static com.symphony.bdk.workflow.api.v1.dto.NodeView.builder;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
class MonitoringApiIntegrationTest extends IntegrationTest {

  private static final String LIST_WORKFLOWS_PATH = "wdk/v1/workflows";
  private static final String LIST_WORKFLOW_INSTANCES_PATH = "wdk/v1/workflows/%s/instances";
  private static final String LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH =
      "wdk/v1/workflows/%s/instances/%s/states";
  private static final String LIST_WORKFLOW_NODES_PATH = "/wdk/v1/workflows/%s/nodes";
  private static final String LIST_WORKFLOW_GLOBAL_VARIABLES = "/wdk/v1/workflows/%s/instances/%s/variables";
  private static final String X_MONITORING_TOKEN_HEADER_KEY = "X-Monitoring-Token";
  private static final String X_MONITORING_TOKEN_HEADER_VALUE = "MONITORING_TOKEN_VALUE";
  private static final String INVALID_X_MONITORING_TOKEN_EXCEPTION_MESSAGE = "Request is not authorised";
  private static final String BAD_WORKFLOW_INSTANCE_STATUS_EXCEPTION_MESSAGE =
      "Workflow instance status %s is not known. Allowed values [Completed, Pending, Failed]";
  private static final String UNKNOWN_WORKFLOW_EXCEPTION_MESSAGE =
      "Either no workflow deployed with id %s, or %s is not an instance of it";

  @Autowired VersionedWorkflowRepository versionedWorkflowRepository;

  @ParameterizedTest
  @CsvSource(value = {LIST_WORKFLOWS_PATH, LIST_WORKFLOW_INSTANCES_PATH, LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH,
      LIST_WORKFLOW_NODES_PATH
  })
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
      LIST_WORKFLOW_NODES_PATH
  })
  void missingTokenHeader(String path) {
    given()
        .contentType(ContentType.JSON)
        .when()
        .get(path)
        .then()
        .assertThat()
        .statusCode(HttpStatus.BAD_REQUEST.value());
  }

  @Test
  void listAllWorkflows() throws Exception {
    final Workflow workflow1 =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-1.swadl.yaml"));
    final Workflow workflow2 =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-2.swadl.yaml"));

    engine.undeployByWorkflowId(workflow1.getId()); // clean any old running instance
    engine.undeployByWorkflowId(workflow2.getId()); // clean any old running instance

    engine.deploy(workflow1);
    engine.deploy(workflow2);

    long version = Instant.now().toEpochMilli();

    VersionedWorkflow v1 = new VersionedWorkflow();
    v1.setWorkflowId(workflow1.getId());
    v1.setVersion(version);
    v1.setPublished(true);
    v1.setSwadl("swadl");
    v1.setActive(true);
    v1.setCreatedBy(123456L);

    VersionedWorkflow v12 = new VersionedWorkflow();
    v12.setWorkflowId(workflow1.getId());
    v12.setVersion(Instant.now().plusSeconds(10).toEpochMilli());
    v12.setPublished(false);
    v12.setSwadl("swadl");
    v12.setActive(false);
    v12.setCreatedBy(123456L);

    VersionedWorkflow v2 = new VersionedWorkflow();
    v2.setWorkflowId(workflow2.getId());
    v2.setVersion(version);
    v2.setPublished(true);
    v2.setActive(true);
    v2.setSwadl("swadl");
    v2.setCreatedBy(123456L);

    versionedWorkflowRepository.save(v1);
    versionedWorkflowRepository.save(v12);
    versionedWorkflowRepository.save(v2);

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(LIST_WORKFLOWS_PATH)
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("size()", is(2))
        .body("[0].id", equalTo("testingWorkflow1"))
        .body("[0].version", equalTo(version))
        .body("[0].createdBy", equalTo(123456))
        .body("[1].id", equalTo("testingWorkflow2"))
        .body("[1].version", equalTo(version))
        .body("[1].createdBy", equalTo(123456));

    engine.undeployByWorkflowId(workflow1.getId());
    engine.undeployByWorkflowId(workflow1.getId());
    versionedWorkflowRepository.deleteAll();
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
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-1.swadl.yaml"));
    final V4Message message = message("Hello!");

    when(messageService.send(anyString(), any(Message.class))).thenReturn(message);

    engine.undeployByWorkflowId(workflow.getId()); // clean any old running instance
    engine.deploy(workflow);
    engine.onEvent(messageReceived("/testingWorkflow1"));

    // Wait for the workflow to get executed
    Thread.sleep(2000);

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCES_PATH, "testingWorkflow1"))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("[0].id", equalTo("testingWorkflow1"))
        .body("[0].status", equalTo("COMPLETED"))
        .body("[0].instanceId", not(isEmptyString()))
        .body("[0].startDate", not(isEmptyString()))
        .body("[0].endDate", not(isEmptyString()))
        .body("[0].duration", not(isEmptyString()));

    engine.undeployByWorkflowId(workflow.getId());
  }

  @Test
  void listWorkflowInstances_pendingStatusAndVersionFilter() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-4.swadl.yaml"));
    long version = Instant.now().toEpochMilli();
    workflow.setVersion(version);

    engine.undeployByWorkflowId(workflow.getId()); // clean any old running instance
    engine.deploy(workflow);
    engine.onEvent(messageReceived("/testingWorkflow4"));

    // Wait for the workflow to get executed
    Thread.sleep(2000);

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCES_PATH + "?status=pending&version=" + version, "testingWorkflow4"))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("[0].id", equalTo("testingWorkflow4"))
        .body("[0].version", equalTo(version))
        .body("[0].status", equalTo("PENDING"))
        .body("[0].instanceId", not(isEmptyString()))
        .body("[0].startDate", not(isEmptyString()))
        .body("[0].endDate", isEmptyOrNullString())
        .body("[0].duration", isEmptyOrNullString());

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCES_PATH + "?version=" + version, "testingWorkflow4"))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("[0].id", equalTo("testingWorkflow4"))
        .body("[0].version", equalTo(version))
        .body("[0].status", equalTo("PENDING"))
        .body("[0].instanceId", not(isEmptyString()))
        .body("[0].startDate", not(isEmptyString()))
        .body("[0].endDate", isEmptyOrNullString())
        .body("[0].duration", isEmptyOrNullString());

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCES_PATH + "?status=completed", "testingWorkflow4"))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("", empty());

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCES_PATH + "?status=failed", "testingWorkflow4"))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("", empty());

    engine.undeployByWorkflowId(workflow.getId());
  }

  @Test
  void listWorkflowInstances_completedStatusFilter() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-3.swadl.yaml"));

    engine.undeployByWorkflowId(workflow.getId()); // clean any old running instance
    engine.deploy(workflow);
    engine.onEvent(messageReceived("/testingWorkflow3"));

    // Wait for the workflow to get executed
    Thread.sleep(2000);

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCES_PATH + "?status=completed", "testingWorkflow3"))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("[0].id", equalTo("testingWorkflow3"))
        .body("[0].status", equalTo("COMPLETED"))
        .body("[0].instanceId", not(isEmptyString()))
        .body("[0].startDate", not(isEmptyString()))
        .body("[0].endDate", not(isEmptyString()))
        .body("[0].duration", not(isEmptyString()));

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCES_PATH + "?status=pending", "testingWorkflow3"))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("", empty());

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCES_PATH + "?status=failed", "testingWorkflow3"))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("", empty());

    engine.undeployByWorkflowId(workflow.getId());
  }

  @Test
  void listWorkflowInstances_failedStatusFilter() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-1.swadl.yaml"));

    when(messageService.send(anyString(), any(Message.class))).thenThrow(new RuntimeException("Unauthorized"));

    engine.undeployByWorkflowId(workflow.getId()); // clean any old running instance
    engine.deploy(workflow);
    engine.onEvent(messageReceived("/testingWorkflow1"));

    // Wait for the workflow to get executed
    Thread.sleep(2000);

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCES_PATH + "?status=failed", "testingWorkflow1"))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("[0].id", equalTo("testingWorkflow1"))
        .body("[0].status", equalTo("FAILED"))
        .body("[0].instanceId", not(isEmptyString()))
        .body("[0].startDate", not(isEmptyString()))
        .body("[0].endDate", not(isEmptyString()))
        .body("[0].duration", not(isEmptyString()));

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCES_PATH + "?status=completed", "testingWorkflow1"))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("", empty());

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCES_PATH + "?status=pending", "testingWorkflow1"))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("", empty());

    engine.undeployByWorkflowId(workflow.getId());
  }

  @Test
  void listWorkflowInstances_unknownStatusFilter() {
    final String path = LIST_WORKFLOW_INSTANCES_PATH + "?status=unknownStatus";
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(path, "testingWorkflow4"))
        .then()
        .assertThat()
        .statusCode(HttpStatus.BAD_REQUEST.value())
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
  void listInstanceStates() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-1.swadl.yaml"));

    final V4Message message = message("Hello!");

    when(messageService.send(anyString(), any(Message.class))).thenReturn(message);

    engine.undeployByWorkflowId(workflow.getId()); // clean any old running instance
    engine.deploy(workflow);
    engine.onEvent(messageReceived("/testingWorkflow1"));

    // Wait for the workflow to get executed
    Thread.sleep(2000);

    String processDefinition = this.getLastProcessInstanceId("testingWorkflow1");

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH, "testingWorkflow1", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("nodes[0].workflowId", equalTo("testingWorkflow1"))
        .body("nodes[0].instanceId", not(empty()))
        .body("nodes[0].nodeId", equalTo("message-received_/testingWorkflow1"))
        .body("nodes[0].type", equalTo("MESSAGE_RECEIVED"))
        .body("nodes[0].group", equalTo("EVENT"))
        .body("nodes[0].startDate", not(empty()))
        .body("nodes[0].endDate", not(empty()))
        .body("nodes[0].duration", not(empty()))
        .body("nodes[0].outputs.message", not(empty()))
        .body("nodes[0].outputs.msgId", not(empty()))

        .body("nodes[1].workflowId", equalTo("testingWorkflow1"))
        .body("nodes[1].instanceId", not(empty()))
        .body("nodes[1].nodeId", equalTo("testingWorkflow1SendMsg1"))
        .body("nodes[1].type", equalTo("SEND_MESSAGE"))
        .body("nodes[1].group", equalTo("ACTIVITY"))
        .body("nodes[1].startDate", not(empty()))
        .body("nodes[1].endDate", not(empty()))
        .body("nodes[1].duration", not(empty()))
        .body("nodes[1].outputs.message", not(empty()))
        .body("nodes[1].outputs.msgId", not(empty()))

        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    engine.undeployByWorkflowId(workflow.getId());
  }

  @Test
  void listInstanceStates_activityWithTimeout() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-with-timeout.swadl.yaml"));

    when(messageService.send(anyString(), any(Message.class))).thenReturn(message("msgId"));

    engine.undeployByWorkflowId(workflow.getId()); // clean any old running instance
    engine.deploy(workflow);
    engine.onEvent(messageReceived("/sendForm"));

    // Wait for the workflow to get executed
    Thread.sleep(2000);

    // No reply to the form in order to let the script activity expire

    String processDefinition = this.getLastProcessInstanceId("testingWorkflowWithTimeout");

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH, "testingWorkflowWithTimeout", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("nodes[0].workflowId", equalTo("testingWorkflowWithTimeout"))
        .body("nodes[0].instanceId", not(empty()))
        .body("nodes[0].nodeId", equalTo("message-received_/sendForm"))
        .body("nodes[0].type", equalTo("MESSAGE_RECEIVED"))
        .body("nodes[0].group", equalTo("EVENT"))
        .body("nodes[0].startDate", not(empty()))
        .body("nodes[0].endDate", not(empty()))
        .body("nodes[0].duration", not(empty()))
        .body("nodes[0].outputs.message", not(empty()))
        .body("nodes[0].outputs.msgId", not(empty()))

        .body("nodes[1].workflowId", equalTo("testingWorkflowWithTimeout"))
        .body("nodes[1].instanceId", not(empty()))
        .body("nodes[1].nodeId", equalTo("formid"))
        .body("nodes[1].type", equalTo("SEND_MESSAGE"))
        .body("nodes[1].group", equalTo("ACTIVITY"))
        .body("nodes[1].startDate", not(empty()))
        .body("nodes[1].endDate", not(empty()))
        .body("nodes[1].duration", not(empty()))
        .body("nodes[1].outputs.message", not(empty()))
        .body("nodes[1].outputs.msgId", not(empty()))

        .body("nodes[2].workflowId", equalTo("testingWorkflowWithTimeout"))
        .body("nodes[2].instanceId", not(empty()))
        .body("nodes[2].nodeId", equalTo("form-reply_formid_timeout"))
        .body("nodes[2].type", equalTo("ACTIVITY_EXPIRED"))
        .body("nodes[2].group", equalTo("EVENT"))
        .body("nodes[2].startDate", not(empty()))
        .body("nodes[2].endDate", not(empty()))
        .body("nodes[2].duration", not(empty()))
        .body("nodes[2].outputs.message", not(empty()))
        .body("nodes[2].outputs.msgId", not(empty()))

        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    engine.undeployByWorkflowId(workflow.getId());
  }

  @Test
  void listInstanceStates_withError() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-1.swadl.yaml"));

    when(messageService.send(anyString(), any(Message.class))).thenThrow(new RuntimeException("Unauthorized"));

    engine.undeployByWorkflowId(workflow.getId()); // clean any old running instance
    engine.deploy(workflow);
    engine.onEvent(messageReceived("/testingWorkflow1"));

    // Wait for the workflow to get executed
    Thread.sleep(2000);

    String processDefinition = this.getLastProcessInstanceId("testingWorkflow1");

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH, "testingWorkflow1", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("nodes[0].workflowId", equalTo("testingWorkflow1"))
        .body("nodes[0].instanceId", not(empty()))
        .body("nodes[0].nodeId", equalTo("message-received_/testingWorkflow1"))
        .body("nodes[0].type", equalTo("MESSAGE_RECEIVED"))
        .body("nodes[0].group", equalTo("EVENT"))
        .body("nodes[0].startDate", not(empty()))
        .body("nodes[0].endDate", not(empty()))
        .body("nodes[0].duration", not(empty()))
        .body("nodes[0].outputs.message", not(empty()))
        .body("nodes[0].outputs.msgId", not(empty()))

        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()))
        .body("error.activityId", equalTo("testingWorkflow1SendMsg1"))
        .body("error.message", equalTo("Unauthorized"))
        .body("error.activityInstId", not(empty()));

    engine.undeployByWorkflowId(workflow.getId());
  }

  @Test
  void listInstanceStates_startedBeforeFilter() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-4.swadl.yaml"));

    engine.undeployByWorkflowId(workflow.getId()); // clean any old running instance
    engine.deploy(workflow);

    Instant beforeFirstSlashInstant = Instant.now();

    engine.onEvent(messageReceived("/testingWorkflow4"));

    // Wait for the first activity to get executed
    Thread.sleep(2000);

    Instant afterFirstSlashInstant = Instant.now();

    engine.onEvent(messageReceived("/continueTestingWorkflow4"));

    // Wait for the second activity to get executed
    Thread.sleep(2000);

    Instant afterSecondSlashInstant = Instant.now();

    String processDefinition = this.getLastProcessInstanceId("testingWorkflow4");

    // No activity started yet
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + "?started_before=" + beforeFirstSlashInstant,
            "testingWorkflow4", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("nodes", hasSize(0))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    // Only the first activity has started
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + "?started_before=" + afterFirstSlashInstant,
            "testingWorkflow4", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("nodes", hasSize(3))
        .body("nodes[0].workflowId", equalTo("testingWorkflow4"))
        .body("nodes[0].instanceId", not(empty()))
        .body("nodes[0].nodeId", equalTo("message-received_/testingWorkflow4"))
        .body("nodes[1].workflowId", equalTo("testingWorkflow4"))
        .body("nodes[1].instanceId", not(empty()))
        .body("nodes[1].nodeId", equalTo("script1TestingWorkflow4"))
        .body("nodes[2].workflowId", equalTo("testingWorkflow4"))
        .body("nodes[2].instanceId", not(empty()))
        .body("nodes[2].nodeId", equalTo("message-received_/continueTestingWorkflow4"))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    // Both workflow's activities have started
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + "?started_before=" + afterSecondSlashInstant,
            "testingWorkflow4", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("nodes", hasSize(4))
        .body("nodes[0].workflowId", equalTo("testingWorkflow4"))
        .body("nodes[0].nodeId", equalTo("message-received_/testingWorkflow4"))
        .body("nodes[0].instanceId", not(empty()))
        .body("nodes[1].workflowId", equalTo("testingWorkflow4"))
        .body("nodes[1].nodeId", equalTo("script1TestingWorkflow4"))
        .body("nodes[1].instanceId", not(empty()))
        .body("nodes[2].workflowId", equalTo("testingWorkflow4"))
        .body("nodes[2].nodeId", equalTo("message-received_/continueTestingWorkflow4"))
        .body("nodes[2].instanceId", not(empty()))
        .body("nodes[3].workflowId", equalTo("testingWorkflow4"))
        .body("nodes[3].instanceId", not(empty()))
        .body("nodes[3].nodeId", equalTo("script2TestingWorkflow4"))
        .body("nodes[3].type", equalTo("EXECUTE_SCRIPT"))
        .body("nodes[3].group", equalTo("ACTIVITY"))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    engine.undeployByWorkflowId(workflow.getId());
  }

  @Test
  void listInstanceStates_startedAfterFilter() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-4.swadl.yaml"));

    engine.undeployByWorkflowId(workflow.getId()); // clean any old running instance
    engine.deploy(workflow);

    Instant beforeFirstSlashInstant = Instant.now();

    engine.onEvent(messageReceived("/testingWorkflow4"));

    // Wait for the first activity to get executed
    Thread.sleep(2000);

    Instant afterFirstSlashInstant = Instant.now();

    engine.onEvent(messageReceived("/continueTestingWorkflow4"));

    // Wait for the second activity to get executed
    Thread.sleep(2000);

    Instant afterSecondSlashInstant = Instant.now();

    String processDefinition = this.getLastProcessInstanceId("testingWorkflow4");

    // Both workflow's activities have started
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + "?started_after=" + beforeFirstSlashInstant,
            "testingWorkflow4", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("nodes", hasSize(4))
        .body("nodes[0].workflowId", equalTo("testingWorkflow4"))
        .body("nodes[0].nodeId", equalTo("message-received_/testingWorkflow4"))
        .body("nodes[0].instanceId", not(empty()))
        .body("nodes[1].workflowId", equalTo("testingWorkflow4"))
        .body("nodes[1].instanceId", not(empty()))
        .body("nodes[1].nodeId", equalTo("script1TestingWorkflow4"))
        .body("nodes[2].workflowId", equalTo("testingWorkflow4"))
        .body("nodes[2].instanceId", not(empty()))
        .body("nodes[2].nodeId", equalTo("message-received_/continueTestingWorkflow4"))
        .body("nodes[3].workflowId", equalTo("testingWorkflow4"))
        .body("nodes[3].instanceId", not(empty()))
        .body("nodes[3].nodeId", equalTo("script2TestingWorkflow4"))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    // Only the second activity has started
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + "?started_after=" + afterFirstSlashInstant,
            "testingWorkflow4", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("nodes", hasSize(1))
        .body("nodes[0].workflowId", equalTo("testingWorkflow4"))
        .body("nodes[0].instanceId", not(empty()))
        .body("nodes[0].nodeId", equalTo("script2TestingWorkflow4"))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    // No activity started
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + "?started_after=" + afterSecondSlashInstant,
            "testingWorkflow4", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("nodes", hasSize(0))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    engine.undeployByWorkflowId(workflow.getId());
  }

  @Test
  void listInstanceStates_finishedBeforeFilter() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-4.swadl.yaml"));

    engine.undeployByWorkflowId(workflow.getId()); // clean any old running instance
    engine.deploy(workflow);

    Instant beforeFirstSlashInstant = Instant.now();

    engine.onEvent(messageReceived("/testingWorkflow4"));

    // Wait for the first activity to get executed
    Thread.sleep(2000);

    Instant afterFirstSlashInstant = Instant.now();

    engine.onEvent(messageReceived("/continueTestingWorkflow4"));

    // Wait for the second activity to get executed
    Thread.sleep(2000);

    Instant afterSecondSlashInstant = Instant.now();

    String processDefinition = this.getLastProcessInstanceId("testingWorkflow4");

    // No activity finished yet
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + "?finished_before=" + beforeFirstSlashInstant,
            "testingWorkflow4", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("nodes", hasSize(0))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    // Only the first activity has finished
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + "?finished_before=" + afterFirstSlashInstant,
            "testingWorkflow4", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("nodes", hasSize(2))
        .body("nodes[0].workflowId", equalTo("testingWorkflow4"))
        .body("nodes[0].instanceId", not(empty()))
        .body("nodes[0].nodeId", equalTo("message-received_/testingWorkflow4"))
        .body("nodes[1].workflowId", equalTo("testingWorkflow4"))
        .body("nodes[1].instanceId", not(empty()))
        .body("nodes[1].nodeId", equalTo("script1TestingWorkflow4"))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    // Both workflow's activities have finished
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + "?finished_before=" + afterSecondSlashInstant,
            "testingWorkflow4", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("nodes", hasSize(4))
        .body("nodes[0].workflowId", equalTo("testingWorkflow4"))
        .body("nodes[0].nodeId", equalTo("message-received_/testingWorkflow4"))
        .body("nodes[0].instanceId", not(empty()))
        .body("nodes[1].workflowId", equalTo("testingWorkflow4"))
        .body("nodes[1].nodeId", equalTo("script1TestingWorkflow4"))
        .body("nodes[1].instanceId", not(empty()))
        .body("nodes[2].workflowId", equalTo("testingWorkflow4"))
        .body("nodes[2].nodeId", equalTo("message-received_/continueTestingWorkflow4"))
        .body("nodes[2].type", equalTo("MESSAGE_RECEIVED"))
        .body("nodes[2].group", equalTo("EVENT"))
        .body("nodes[2].instanceId", not(empty()))
        .body("nodes[3].workflowId", equalTo("testingWorkflow4"))
        .body("nodes[3].instanceId", not(empty()))
        .body("nodes[3].nodeId", equalTo("script2TestingWorkflow4"))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    engine.undeployByWorkflowId(workflow.getId());
  }

  @Test
  void listInstanceStates_finishedAfterFilter() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-4.swadl.yaml"));

    engine.undeployByWorkflowId(workflow.getId()); // clean any old running instance
    engine.deploy(workflow);

    Instant beforeFirstSlashInstant = Instant.now();

    engine.onEvent(messageReceived("/testingWorkflow4"));

    // Wait for the first activity to get executed
    Thread.sleep(2000);

    Instant afterFirstSlashInstant = Instant.now();

    engine.onEvent(messageReceived("/continueTestingWorkflow4"));

    // Wait for the second activity to get executed
    Thread.sleep(2000);

    Instant afterSecondSlashInstant = Instant.now();

    String processDefinition = this.getLastProcessInstanceId("testingWorkflow4");

    // Both workflow's activities have finished
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + "?finished_after=" + beforeFirstSlashInstant,
            "testingWorkflow4", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("nodes", hasSize(4))
        .body("nodes[0].workflowId", equalTo("testingWorkflow4"))
        .body("nodes[0].nodeId", equalTo("message-received_/testingWorkflow4"))
        .body("nodes[0].instanceId", not(empty()))
        .body("nodes[1].workflowId", equalTo("testingWorkflow4"))
        .body("nodes[1].nodeId", equalTo("script1TestingWorkflow4"))
        .body("nodes[1].instanceId", not(empty()))
        .body("nodes[2].workflowId", equalTo("testingWorkflow4"))
        .body("nodes[2].instanceId", not(empty()))
        .body("nodes[2].nodeId", equalTo("message-received_/continueTestingWorkflow4"))
        .body("nodes[3].workflowId", equalTo("testingWorkflow4"))
        .body("nodes[3].instanceId", not(empty()))
        .body("nodes[3].nodeId", equalTo("script2TestingWorkflow4"))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    // Only the second activity has finished
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + "?finished_after=" + afterFirstSlashInstant,
            "testingWorkflow4", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("nodes", hasSize(2))
        .body("nodes[0].workflowId", equalTo("testingWorkflow4"))
        .body("nodes[0].instanceId", not(empty()))
        .body("nodes[0].nodeId", equalTo("message-received_/continueTestingWorkflow4"))
        .body("nodes[1].workflowId", equalTo("testingWorkflow4"))
        .body("nodes[1].instanceId", not(empty()))
        .body("nodes[1].nodeId", equalTo("script2TestingWorkflow4"))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    // No activity finished yet
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + "?finished_after=" + afterSecondSlashInstant,
            "testingWorkflow4", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())

        .body("nodes", hasSize(0))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    engine.undeployByWorkflowId(workflow.getId());
  }

  @ParameterizedTest
  @CsvSource({"?started_before=INVALID_INSTANT", "?started_after=INVALID_INSTANT", "?finished_before=INVALID_INSTANT",
      "?finished_after=INVALID_INSTANT"})
  void listInstanceActivities_invalidFilter(String queryParam) {
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH + queryParam, "testingWorkflow4", ""))
        .then()
        .assertThat()
        .statusCode(HttpStatus.BAD_REQUEST.value());
  }

  @Test
  void listInstanceStates_unknownWorkflowId_unknownInstanceId() {
    final String unknownWorkflowId = "unknownWorkflowId";
    final String unknownInstanceId = "unknownInstanceId";
    final String expectedErrorMsg =
        String.format(UNKNOWN_WORKFLOW_EXCEPTION_MESSAGE, unknownWorkflowId, unknownInstanceId);

    engine.undeployByWorkflowId(unknownWorkflowId);

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
  void listWorkflowStatesDefinitions() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-definition.swadl.yaml"));

    engine.undeployByWorkflowId(workflow.getId()); // clean any old running instance
    engine.deploy(workflow);

    // Wait for the workflow to get executed
    Thread.sleep(2000);

    Response response = given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_NODES_PATH, "testingWorkflow1"))
        .thenReturn();

    // actual flow nodes
    ObjectMapper objectMapper = new ObjectMapper();
    List<NodeView> nodeViews = new ArrayList<>();
    List<Object> flowNodes = response.body().jsonPath().getList("flowNodes");

    flowNodes.forEach(flowNode -> {
      try {
        nodeViews.add(
            objectMapper.readValue(objectMapper.writeValueAsString(flowNode), NodeView.class));
      } catch (JsonProcessingException e) {
        e.printStackTrace();
        fail("Unexpected error when converting api response to TaskDefinitionView class");
      }
    });

    // expected flow nodes
    NodeView expectedSendMessageActivity1 = builder()
        .nodeId("testingWorkflow1SendMsg1")
        .type(WorkflowNodeTypeHelper.toType("SEND_MESSAGE"))
        .group(WorkflowNodeTypeHelper.toGroup("SEND_MESSAGE"))
        .parents(Collections.singletonList("message-received_/testingWorkflow1"))
        .children(Collections.singletonList(ChildView.of("sendForm")))
        .build();

    NodeView expectedSendMessageActivity2 = builder()
        .nodeId("sendForm")
        .type(WorkflowNodeTypeHelper.toType("SEND_MESSAGE"))
        .group(WorkflowNodeTypeHelper.toGroup("SEND_MESSAGE"))
        .parents(Collections.singletonList("testingWorkflow1SendMsg1"))
        .children(List.of(ChildView.of("form-reply_sendForm"), ChildView.of("form-reply_sendForm_timeout", "expired")))
        .build();

    NodeView expectedMessageReceivedEventTask = builder()
        .nodeId("message-received_/testingWorkflow1")
        .type(WorkflowNodeTypeHelper.toType("MESSAGE_RECEIVED_EVENT"))
        .group(WorkflowNodeTypeHelper.toGroup("MESSAGE_RECEIVED_EVENT"))
        .parents(Collections.emptyList())
        .children(Collections.singletonList(ChildView.of("testingWorkflow1SendMsg1")))
        .build();

    NodeView expectedFormRepliedEventTask = NodeView.builder()
        .nodeId("form-reply_sendForm")
        .type(WorkflowNodeTypeHelper.toType("FORM_REPLIED_EVENT"))
        .group(WorkflowNodeTypeHelper.toGroup("FORM_REPLIED_EVENT"))
        .parents(Collections.singletonList("sendForm"))
        .children(Collections.singletonList(ChildView.of("receiveForm")))
        .build();

    NodeView expectedFormRepliedTimeoutEventTask = NodeView.builder()
        .nodeId("form-reply_sendForm_timeout")
        .type(WorkflowNodeTypeHelper.toType("ACTIVITY_EXPIRED_EVENT"))
        .group(WorkflowNodeTypeHelper.toGroup("ACTIVITY_EXPIRED_EVENT"))
        .parents(Collections.singletonList("sendForm"))
        .children(Collections.emptyList())
        .build();

    NodeView expectedReceiveFormTask = NodeView.builder()
        .nodeId("receiveForm")
        .type(WorkflowNodeTypeHelper.toType("SEND_MESSAGE"))
        .group(WorkflowNodeTypeHelper.toGroup("SEND_MESSAGE"))
        .parents(List.of("form-reply_sendForm"))
        .children(Collections.emptyList())
        .build();

    List<NodeView> expectedTaskDefinitions =
        Arrays.asList(expectedSendMessageActivity1, expectedSendMessageActivity2,
            expectedMessageReceivedEventTask, expectedFormRepliedEventTask, expectedFormRepliedTimeoutEventTask,
            expectedReceiveFormTask);

    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(response.body().jsonPath().getString("workflowId")).isEqualTo("testingWorkflow1");
    assertThat(response.body().jsonPath().getMap("variables")).isEmpty();

    assertThat(nodeViews)
        .hasSameSizeAs(expectedTaskDefinitions)
        .hasSameElementsAs(expectedTaskDefinitions);

    engine.undeployByWorkflowId(workflow.getId());
  }

  @Test
  void listWorkflow_oneOf_activityFailedEvent_Definitions() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/oneof-activityfailed-definition.swadl.yaml"));

    engine.undeployByWorkflowId(workflow.getId()); // clean any old running instance
    engine.deploy(workflow);

    // Wait for the workflow to get executed
    Thread.sleep(1000);

    Response response = given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_NODES_PATH, "on-activity-failed-one-of"))
        .thenReturn();

    // actual flow nodes
    ObjectMapper objectMapper = new ObjectMapper();
    List<NodeView> nodeViews = new ArrayList<>();
    List<Object> flowNodes = response.body().jsonPath().getList("flowNodes");

    flowNodes.forEach(flowNode -> {
      try {
        nodeViews.add(
            objectMapper.readValue(objectMapper.writeValueAsString(flowNode), NodeView.class));
      } catch (JsonProcessingException e) {
        e.printStackTrace();
        fail("Unexpected error when converting api response to TaskDefinitionView class");
      }
    });

    // expected flow nodes
    NodeView expectedSendMessageActivity1 = builder()
        .nodeId("first")
        .type(WorkflowNodeTypeHelper.toType("SEND_MESSAGE"))
        .group(WorkflowNodeTypeHelper.toGroup("SEND_MESSAGE"))
        .parents(Collections.singletonList("message-received_/failure"))
        .children(List.of(ChildView.of("second"), ChildView.of("fallback", "failed")))
        .build();

    NodeView expectedSendMessageActivity2 = builder()
        .nodeId("second")
        .type(WorkflowNodeTypeHelper.toType("SEND_MESSAGE"))
        .group(WorkflowNodeTypeHelper.toGroup("SEND_MESSAGE"))
        .parents(Collections.singletonList("first"))
        .children(List.of(ChildView.of("fallback", "failed")))
        .build();

    NodeView expectedSendMessageFallback = builder()
        .nodeId("fallback")
        .type(WorkflowNodeTypeHelper.toType("SEND_MESSAGE"))
        .group(WorkflowNodeTypeHelper.toGroup("SEND_MESSAGE"))
        .parents(List.of("first", "second"))
        .children(Collections.emptyList())
        .build();

    NodeView expectedMessageReceivedEventTask = builder()
        .nodeId("message-received_/failure")
        .type(WorkflowNodeTypeHelper.toType("MESSAGE_RECEIVED_EVENT"))
        .group(WorkflowNodeTypeHelper.toGroup("MESSAGE_RECEIVED_EVENT"))
        .parents(Collections.emptyList())
        .children(Collections.singletonList(ChildView.of("first")))
        .build();

    List<NodeView> expectedTaskDefinitions =
        Arrays.asList(expectedSendMessageActivity1, expectedSendMessageActivity2,
            expectedSendMessageFallback,
            expectedMessageReceivedEventTask);

    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(response.body().jsonPath().getString("workflowId")).isEqualTo("on-activity-failed-one-of");
    assertThat(response.body().jsonPath().getMap("variables")).isEmpty();

    assertThat(nodeViews)
        .hasSameSizeAs(expectedTaskDefinitions)
        .hasSameElementsAs(expectedTaskDefinitions);

    engine.undeployByWorkflowId(workflow.getId());
  }

  @Test
  void listWorkflow_oneOf_allOf_gateway_definitions() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/oneof-allof-gateway-definition.swadl.yaml"));

    engine.undeployByWorkflowId(workflow.getId()); // clean any old running instance
    engine.deploy(workflow);

    // Wait for the workflow to get executed
    Thread.sleep(1000);

    Response response = given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_NODES_PATH, "all-of-messages-received"))
        .thenReturn();

    // actual flow nodes
    ObjectMapper objectMapper = new ObjectMapper();
    List<NodeView> nodeViews = new ArrayList<>();
    List<Object> flowNodes = response.body().jsonPath().getList("flowNodes");

    flowNodes.forEach(flowNode -> {
      try {
        nodeViews.add(
            objectMapper.readValue(objectMapper.writeValueAsString(flowNode), NodeView.class));
      } catch (JsonProcessingException e) {
        e.printStackTrace();
        fail("Unexpected error when converting api response to TaskDefinitionView class");
      }
    });

    // expected flow nodes
    NodeView expectedSendMessageStart = builder()
        .nodeId("start")
        .type(WorkflowNodeTypeHelper.toType("SEND_MESSAGE"))
        .group(WorkflowNodeTypeHelper.toGroup("SEND_MESSAGE"))
        .parents(Collections.singletonList("message-received_/start"))
        .children(List.of(ChildView.of("scriptTrue", "${variables.allOf == true}"), ChildView.of("scriptFalse")))
        .build();

    NodeView expectedScriptTrue = builder()
        .nodeId("scriptTrue")
        .type(WorkflowNodeTypeHelper.toType("EXECUTE_SCRIPT"))
        .group(WorkflowNodeTypeHelper.toGroup("EXECUTE_SCRIPT"))
        .parents(Collections.singletonList("start"))
        .children(List.of(ChildView.of("message-received_/message"), ChildView.of("user-joined-room"),
            ChildView.of("endMessage_join_gateway")))
        .build();

    NodeView expectedScriptFalse = builder()
        .nodeId("scriptFalse")
        .type(WorkflowNodeTypeHelper.toType("EXECUTE_SCRIPT"))
        .group(WorkflowNodeTypeHelper.toGroup("EXECUTE_SCRIPT"))
        .parents(List.of("start"))
        .children(Collections.emptyList())
        .build();

    NodeView expectedMessageReceivedEventTask = builder()
        .nodeId("message-received_/start")
        .type(WorkflowNodeTypeHelper.toType("MESSAGE_RECEIVED_EVENT"))
        .group(WorkflowNodeTypeHelper.toGroup("MESSAGE_RECEIVED_EVENT"))
        .parents(Collections.emptyList())
        .children(Collections.singletonList(ChildView.of("start")))
        .build();

    NodeView expectedReceiveMessageEnd = builder()
        .nodeId("message-received_/message")
        .type(WorkflowNodeTypeHelper.toType("MESSAGE_RECEIVED_EVENT"))
        .group(WorkflowNodeTypeHelper.toGroup("MESSAGE_RECEIVED_EVENT"))
        .parents(List.of("scriptTrue"))
        .children(List.of(ChildView.of("endMessage_join_gateway")))
        .build();

    NodeView expectedUserJoinedGateway = builder()
        .nodeId("user-joined-room")
        .type(WorkflowNodeTypeHelper.toType("USER_JOINED_ROOM_EVENT"))
        .group(WorkflowNodeTypeHelper.toGroup("USER_JOINED_ROOM_EVENT"))
        .parents(Collections.singletonList("scriptTrue"))
        .children(List.of(ChildView.of("endMessage_join_gateway")))
        .build();

    NodeView expectedJoinGateway = builder()
        .nodeId("endMessage_join_gateway")
        .type(WorkflowNodeTypeHelper.toType("JOIN_GATEWAY"))
        .group(WorkflowNodeTypeHelper.toGroup("JOIN_GATEWAY"))
        .parents(List.of("message-received_/message", "user-joined-room", "scriptTrue"))
        .children(List.of(ChildView.of("endMessage")))
        .build();

    NodeView expectedSendMessageEnd = builder()
        .nodeId("endMessage")
        .type(WorkflowNodeTypeHelper.toType("SEND_MESSAGE"))
        .group(WorkflowNodeTypeHelper.toGroup("SEND_MESSAGE"))
        .parents(List.of("endMessage_join_gateway"))
        .children(Collections.emptyList())
        .build();

    List<NodeView> expectedTaskDefinitions =
        Arrays.asList(expectedMessageReceivedEventTask, expectedSendMessageStart, expectedScriptTrue,
            expectedScriptFalse, expectedReceiveMessageEnd, expectedUserJoinedGateway, expectedJoinGateway,
            expectedSendMessageEnd);

    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(response.body().jsonPath().getString("workflowId")).isEqualTo("all-of-messages-received");
    assertThat(response.body().jsonPath().getMap("variables")).hasSize(1);

    assertThat(nodeViews)
        .hasSameSizeAs(expectedTaskDefinitions)
        .hasSameElementsAs(expectedTaskDefinitions);

    engine.undeployByWorkflowId(workflow.getId());
  }

  @Test
  void listWorkflow_allOf_onActivities_definitions() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/allof-on-activities-definition.swadl.yaml"));

    engine.undeployByWorkflowId(workflow.getId()); // clean any old running instance
    engine.deploy(workflow);

    // Wait for the workflow to get executed
    Thread.sleep(1000);

    Response response = given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_NODES_PATH, "diagram-test"))
        .thenReturn();

    // actual flow nodes
    ObjectMapper objectMapper = new ObjectMapper();
    List<NodeView> nodeViews = new ArrayList<>();
    List<Object> flowNodes = response.body().jsonPath().getList("flowNodes");

    flowNodes.forEach(flowNode -> {
      try {
        nodeViews.add(
            objectMapper.readValue(objectMapper.writeValueAsString(flowNode), NodeView.class));
      } catch (JsonProcessingException e) {
        e.printStackTrace();
        fail("Unexpected error when converting api response to TaskDefinitionView class");
      }
    });

    NodeView messageReceived = builder()
        .nodeId("message-received_diagram")
        .type(WorkflowNodeTypeHelper.toType("MESSAGE_RECEIVED_EVENT"))
        .group(WorkflowNodeTypeHelper.toGroup("MESSAGE_RECEIVED_EVENT"))
        .parents(Collections.emptyList())
        .children(List.of(ChildView.of("init")))
        .build();

    // expected flow nodes
    NodeView expectedSendMessageStart = builder()
        .nodeId("init")
        .type(WorkflowNodeTypeHelper.toType("SEND_MESSAGE"))
        .group(WorkflowNodeTypeHelper.toGroup("SEND_MESSAGE"))
        .parents(Collections.singletonList("message-received_diagram"))
        .children(List.of(ChildView.of("abc"), ChildView.of("def")))
        .build();

    NodeView abc = builder()
        .nodeId("abc")
        .type(WorkflowNodeTypeHelper.toType("EXECUTE_SCRIPT"))
        .group(WorkflowNodeTypeHelper.toGroup("EXECUTE_SCRIPT"))
        .parents(Collections.singletonList("init"))
        .children(List.of(ChildView.of("completed_join_gateway")))
        .build();

    NodeView def = builder()
        .nodeId("def")
        .type(WorkflowNodeTypeHelper.toType("EXECUTE_SCRIPT"))
        .group(WorkflowNodeTypeHelper.toGroup("EXECUTE_SCRIPT"))
        .parents(Collections.singletonList("init"))
        .children(List.of(ChildView.of("completed_join_gateway")))
        .build();

    NodeView gateway = builder()
        .nodeId("completed_join_gateway")
        .type(WorkflowNodeTypeHelper.toType("JOIN_GATEWAY"))
        .group(WorkflowNodeTypeHelper.toGroup("JOIN_GATEWAY"))
        .parents(List.of("abc", "def"))
        .children(Collections.singletonList(ChildView.of("completed")))
        .build();

    NodeView expectedSendMessageEnd = builder()
        .nodeId("completed")
        .type(WorkflowNodeTypeHelper.toType("SEND_MESSAGE"))
        .group(WorkflowNodeTypeHelper.toGroup("SEND_MESSAGE"))
        .parents(List.of("completed_join_gateway"))
        .children(Collections.emptyList())
        .build();

    List<NodeView> expectedTaskDefinitions =
        Arrays.asList(messageReceived, expectedSendMessageStart, abc, def, gateway, expectedSendMessageEnd);

    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(response.body().jsonPath().getString("workflowId")).isEqualTo("diagram-test");
    assertThat(response.body().jsonPath().getMap("variables")).isEmpty();

    assertThat(nodeViews)
        .hasSameSizeAs(expectedTaskDefinitions)
        .hasSameElementsAs(expectedTaskDefinitions);

    engine.undeployByWorkflowId(workflow.getId());
  }

  @Test
  void listWorkflowActivitiesDefinitions_unknownWorkflowId() {
    final String unknownWorkflowId = "unknownWorkflowId";
    final String expectedErrorMsg =
        String.format("No workflow with id '%s' and version 'null' is found", unknownWorkflowId);

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_NODES_PATH, unknownWorkflowId))
        .then()
        .assertThat()
        .statusCode(HttpStatus.NOT_FOUND.value())
        .body("message", equalTo(expectedErrorMsg));
  }

  // TODO: Flaky test
  @Disabled("Flaky test: for some reason, the two first updates have the same revision and the order is not guaranteed")
  void listWorkflowGlobalVariables() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-5.swadl.yaml"));

    engine.undeployByWorkflowId(workflow.getId()); // clean any old running instance
    engine.deploy(workflow);
    engine.onEvent(messageReceived("/testingWorkflow5"));

    // Wait for the first activity to get executed
    Thread.sleep(2000);

    engine.onEvent(messageReceived("/continueTestingWorkflow5"));

    // Wait for the second activity to get executed
    Thread.sleep(2000);

    String processDefinition = this.getLastProcessInstanceId("testingWorkflow5");

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_GLOBAL_VARIABLES, "testingWorkflow5", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("", hasSize(3))

        .body("[1].revision", equalTo(0))
        .body("[1].outputs.key1", equalTo("value_1_updated"))
        .body("[1].outputs.key2", equalTo("value_2_initial"))
        .body("[1].outputs.key3", equalTo("value_3_added"))
        .body("[1].updateTime", not(empty()))

        .body("[0].revision", equalTo(0))
        .body("[0].outputs.key1", equalTo("value_1_initial"))
        .body("[0].outputs.key2", equalTo("value_2_initial"))
        .body("[0].updateTime", not(empty()))

        .body("[2].revision", equalTo(1))
        .body("[2].outputs.key1", equalTo("value_1_updated"))
        .body("[2].outputs.key2", equalTo("value_2_initial"))
        .body("[2].outputs.key3", equalTo("value_3_added"))
        .body("[2].outputs.key4", equalTo("value_4_added"))
        .body("[2].updateTime", not(empty()));

    engine.undeployByWorkflowId(workflow.getId());
  }

  @Test
  void listWorkflowGlobalVariables_updatedBeforeFilter() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-5.swadl.yaml"));

    engine.undeployByWorkflowId(workflow.getId()); // clean any old running instance
    engine.deploy(workflow);

    Instant beforeFirstSlashInstant = Instant.now();
    engine.onEvent(messageReceived("/testingWorkflow5"));

    // Wait for the first activity to get executed
    Thread.sleep(2000);

    Instant afterFirstSlashInstant = Instant.now();
    engine.onEvent(messageReceived("/continueTestingWorkflow5"));

    // Wait for the second activity to get executed
    Thread.sleep(2000);

    Instant afterSecondSlashInstant = Instant.now();
    String processDefinition = this.getLastProcessInstanceId("testingWorkflow5");

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_GLOBAL_VARIABLES + "?updated_before=" + beforeFirstSlashInstant,
            "testingWorkflow5", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("", empty());

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_GLOBAL_VARIABLES + "?updated_before=" + afterFirstSlashInstant,
            "testingWorkflow5", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("", hasSize(2))
        .body("[0].revision", equalTo(0))
        .body("[1].revision", equalTo(0));

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_GLOBAL_VARIABLES + "?updated_before=" + afterSecondSlashInstant,
            "testingWorkflow5", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("", hasSize(3))
        .body("[0].revision", equalTo(0))
        .body("[1].revision", equalTo(0))
        .body("[2].revision", equalTo(1));

    engine.undeployByWorkflowId(workflow.getId());
  }

  @Test
  void listWorkflowGlobalVariables_updatedAfterFilter() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-5.swadl.yaml"));

    engine.undeployByWorkflowId(workflow.getId()); // clean any old running instance
    engine.deploy(workflow);

    Instant beforeFirstSlashInstant = Instant.now();
    engine.onEvent(messageReceived("/testingWorkflow5"));

    // Wait for the first activity to get executed
    Thread.sleep(2000);

    Instant afterFirstSlashInstant = Instant.now();
    engine.onEvent(messageReceived("/continueTestingWorkflow5"));

    // Wait for the second activity to get executed
    Thread.sleep(2000);

    Instant afterSecondSlashInstant = Instant.now();
    String processDefinition = this.getLastProcessInstanceId("testingWorkflow5");

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_GLOBAL_VARIABLES + "?updated_after=" + beforeFirstSlashInstant,
            "testingWorkflow5", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("", hasSize(3))
        .body("[0].revision", equalTo(0))
        .body("[1].revision", equalTo(0))
        .body("[2].revision", equalTo(1));

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_GLOBAL_VARIABLES + "?updated_after=" + afterFirstSlashInstant,
            "testingWorkflow5", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("", hasSize(1))
        .body("[0].revision", equalTo(1));

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_GLOBAL_VARIABLES + "?updated_after=" + afterSecondSlashInstant,
            "testingWorkflow5", processDefinition))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("", empty());

    engine.undeployByWorkflowId(workflow.getId());
  }

  @ParameterizedTest
  @CsvSource({"?updated_before=INVALID", "?updated_after=INVALID"})
  void listWorkflowGlobalVariables_invalidFilter(String queryParam) {
    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_GLOBAL_VARIABLES + queryParam, "testingWorkflow5", ""))
        .then()
        .assertThat()
        .statusCode(HttpStatus.BAD_REQUEST.value());
  }

  @Test
  void listWorkflowGlobalVariables_unknownWorkflowId_unknownInstanceId() {
    final String unknownWorkflowId = "unknownWorkflowId";
    final String unknownInstanceId = "unknownInstanceId";
    final String expectedErrorMsg =
        String.format(UNKNOWN_WORKFLOW_EXCEPTION_MESSAGE, unknownWorkflowId, unknownInstanceId);

    engine.undeployAll(); // clean any old running instance

    given()
        .header(X_MONITORING_TOKEN_HEADER_KEY, X_MONITORING_TOKEN_HEADER_VALUE)
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_GLOBAL_VARIABLES, unknownWorkflowId, unknownInstanceId))
        .then()
        .assertThat()
        .statusCode(HttpStatus.NOT_FOUND.value())
        .body("message", equalTo(expectedErrorMsg));
  }

  private String getLastProcessInstanceId(String workflowId) {
    Optional<String> processDefinition = historyService.createHistoricProcessInstanceQuery()
        .processDefinitionKey(workflowId)
        .orderByProcessInstanceStartTime()
        .desc() // if many instances are found, always return the latest
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
