package com.symphony.bdk.workflow;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.api.v1.dto.ActivityDefinitionView;
import com.symphony.bdk.workflow.api.v1.dto.EventDefinitionView;
import com.symphony.bdk.workflow.api.v1.dto.TaskDefinitionView;
import com.symphony.bdk.workflow.api.v1.dto.TaskTypeEnum;
import com.symphony.bdk.workflow.monitoring.service.MonitoringService;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration
@RunWith(SpringRunner.class)
public class MonitoringApiIntegrationTest extends IntegrationTest {

  private static final String LIST_WORKFLOWS_PATH = "wdk/v1/workflows/";
  private static final String LIST_WORKFLOW_INSTANCES_PATH = "wdk/v1/workflows/%s/instances";
  private static final String LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH =
      "wdk/v1/workflows/%s/instances/%s/activities";
  private static final String LIST_WORKFLOW_DEFINITIONS_PATH = "/wdk/v1/workflows/%s/definitions";

  @LocalServerPort
  private int port;

  @Autowired MonitoringService monitoringService;

  @Test
  public void listAllWorkflows() throws Exception {
    final Workflow workflow1 =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-1.swadl.yaml"));
    final Workflow workflow2 =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-2.swadl.yaml"));
    final JsonPath expectedJson = new JsonPath(
        getClass().getResourceAsStream("/monitoring/expected/list-workflows-response-payload.json"));

    engine.deploy(workflow1);
    engine.deploy(workflow2);

    given()
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
  public void listAllWorkflows_noWorkflowDeployed() {
    engine.undeployAll();

    given()
        .contentType(ContentType.JSON)
        .when()
        .get(LIST_WORKFLOWS_PATH)
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("", empty());
  }

  @Test
  public void listWorkflowInstances() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-3.swadl.yaml"));
    final V4Message message = message("Hello!");

    when(messageService.send(anyString(), any(Message.class))).thenReturn(message);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/testingWorkflow3"));

    // Wait for the workflow to get deployed
    Thread.sleep(2000);

    given()
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
  public void listWorkflowInstances_unknownWorkflow() {
    engine.undeployAll();

    given()
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCES_PATH, "testingWorkflow1"))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("", empty());
  }

  @Test
  public void listInstanceActivities() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-4.swadl.yaml"));

    final V4Message message = message("Hello!");

    when(messageService.send(anyString(), any(Message.class))).thenReturn(message);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/testingWorkflow4"));

    // Wait for the workflow to get deployed
    Thread.sleep(2000);

    Optional<String> processDefinition = historyService.createHistoricProcessInstanceQuery()
        .processDefinitionKey("testingWorkflow4")
        .list()
        .stream()
        .map(HistoricProcessInstance::getId)
        .findFirst();

    if (processDefinition.isEmpty()) {
      fail("At least one process definition should have been found.");
    }

    given()
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH, "testingWorkflow4", processDefinition.get()))
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
        .body("activities[0].variables.outputs.message", not(empty()))
        .body("activities[0].variables.outputs.msgId", not(empty()))
        .body("activities[0].variables.revision", equalTo(0))
        .body("activities[0].variables.updateTime", not(empty()))

        .body("activities[1].workflowId", equalTo("testingWorkflow4"))
        .body("activities[1].instanceId", not(empty()))
        .body("activities[1].activityId", equalTo("testingWorkflow4SendMsg2"))
        .body("activities[1].type", equalTo("SEND_MESSAGE_ACTIVITY"))
        .body("activities[1].startDate", not(empty()))
        .body("activities[1].endDate", not(empty()))
        .body("activities[1].duration", not(empty()))
        .body("activities[1].variables.outputs.message", not(empty()))
        .body("activities[1].variables.outputs.msgId", not(empty()))
        .body("activities[1].variables.revision", equalTo(0))
        .body("activities[1].variables.updateTime", not(empty()))


        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", not(empty()));

    engine.undeploy("testingWorkflow4");
  }

  @Test
  public void listInstanceActivities_unknownWorkflowId() {
    engine.undeploy("testingWorkflow1");

    given()
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_INSTANCE_ACTIVITIES_PATH, "testingWorkflow1", "unknownInstanceId"))
        .then()
        .assertThat()
        .body("activities", equalTo(Collections.EMPTY_LIST))
        .body("globalVariables.outputs", equalTo(Collections.EMPTY_MAP))
        .body("globalVariables.revision", equalTo(0))
        .body("globalVariables.updateTime", isEmptyOrNullString());
  }

  @Test
  public void listWorkflowActivitiesDefinitions() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/monitoring/testing-workflow-5.swadl.yaml"));

    engine.deploy(workflow);

    // Wait for the workflow to get deployed
    Thread.sleep(2000);

    Response response = given()
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_DEFINITIONS_PATH, "testingWorkflow5"))
        .thenReturn();

    // actual flow nodes
    List<TaskDefinitionView> taskDefinitionViews =
        toTaskDefinitionViewList((response.body().jsonPath().getList("flowNodes")));

    // expected flow nodes
    ActivityDefinitionView expectedActivityDefinitionView1 =
        new ActivityDefinitionView(TaskDefinitionView.builder()
            .type(TaskTypeEnum.SEND_MESSAGE_ACTIVITY)
            .parents(Collections.singletonList("message-received_/testingWorkflow5"))
            .children(Collections.singletonList("testingWorkflow5SendMsg2"))
            .build(), "testingWorkflow5SendMsg1");

    ActivityDefinitionView expectedActivityDefinitionView2 =
        new ActivityDefinitionView(TaskDefinitionView.builder()
            .type(TaskTypeEnum.SEND_MESSAGE_ACTIVITY)
            .parents(Collections.singletonList("testingWorkflow5SendMsg1"))
            .children(Collections.emptyList())
            .build(), "testingWorkflow5SendMsg2");

    EventDefinitionView expectedEventDefinitionView =
        new EventDefinitionView(TaskDefinitionView.builder()
            .type(TaskTypeEnum.MESSAGE_RECEIVED_EVENT)
            .parents(Collections.emptyList())
            .children(Collections.singletonList("testingWorkflow5SendMsg1"))
            .build());

    List<TaskDefinitionView> expectedTaskDefinitions =
        Arrays.asList(expectedActivityDefinitionView1, expectedActivityDefinitionView2, expectedEventDefinitionView);

    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(response.body().jsonPath().getString("workflowId")).isEqualTo("testingWorkflow5");
    assertThat(response.body().jsonPath().getList("variables")).isEmpty();
    assertThat(taskDefinitionViews)
        .hasSameSizeAs(expectedTaskDefinitions)
        .hasSameElementsAs(expectedTaskDefinitions);

    engine.undeploy("testingWorkflow5");
  }

  @Test
  public void AlistWorkflowActivitiesDefinitions_unknownWorkflowId() {
    final JsonPath expectedJson = new JsonPath(
        getClass().getResourceAsStream("/monitoring/expected/unknown-workflow-definition-response-payload.json"));

    given()
        .contentType(ContentType.JSON)
        .when()
        .get(String.format(LIST_WORKFLOW_DEFINITIONS_PATH, "unknownWorkflowId"))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .body("", equalTo(expectedJson.getMap("")));
  }

  @Before
  public void setUp() {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;

    when(bdkGateway.messages()).thenReturn(messageService);
  }

  private List<TaskDefinitionView> toTaskDefinitionViewList(List<Object> objects)
      throws JsonProcessingException, JSONException {
    ObjectMapper objectMapper = new ObjectMapper();
    List<TaskDefinitionView> taskDefinitionViews = new ArrayList<>();

    for (Object flowNode : objects) {
      JSONObject jsonObject = new JSONObject(objectMapper.writer().writeValueAsString(flowNode));
      List<String> children = objectMapper.readValue(jsonObject.get("children").toString(), List.class);
      List<String> parents = objectMapper.readValue(jsonObject.get("parents").toString(), List.class);

      TaskDefinitionView taskDefinitionView = TaskDefinitionView.builder()
          .type(TaskTypeEnum.valueOf(jsonObject.getString("type")))
          .children(children)
          .parents(parents)
          .build();
      if (jsonObject.isNull("activityId")) {
        taskDefinitionViews.add(new EventDefinitionView(taskDefinitionView));
      } else {
        taskDefinitionViews.add(new ActivityDefinitionView(taskDefinitionView, jsonObject.getString("activityId")));
      }
    }

    return taskDefinitionViews;
  }
}

