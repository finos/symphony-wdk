package com.symphony.bdk.workflow.api.v1.controller;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "A NP should make the test fail")
public class WorkflowsMonitoringApiIntegrationTest extends ApiIntegrationTest {
  private static final String VALID_SWADL_1 = "id: valid-dummy-workflow-1\n"
      + "activities:\n"
      + "  - send-message: \n"
      + "      id: script0\n"
      + "      on:\n"
      + "        message-received:\n"
      + "          content: /valid-dummy-workflow-1\n"
      + "      content: Started\n"
      + "      to: \n"
      + "        stream-id: \"123\"\n"
      + "  - execute-script:\n"
      + "      id: script1\n"
      + "      on:\n"
      + "        message-received:\n"
      + "          content: /continue\n"
      + "      script: |\n";

  private static final String VALID_SWADL_2 = "id: valid-dummy-workflow-2\n"
      + "activities:\n"
      + "  - send-message: \n"
      + "      id: script0\n"
      + "      on:\n"
      + "        message-received:\n"
      + "          content: /valid-dummy-workflow-2\n"
      + "      content: OK\n"
      + "      to: \n"
      + "        stream-id: \"123\"";

  @Test
  @Order(1)
  void deployWorkflowsTest() {
    // deploy 2 workflows
    saveAndDeploy(VALID_SWADL_1, "");
    saveAndDeploy(VALID_SWADL_2, "");

    // list workflows
    ResponseEntity<List<WorkflowView>> allWorkflowsResponse = listAllWorkflows();
    assertThat(allWorkflowsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(allWorkflowsResponse.getBody()).hasSize(2);
    List<String> deployedWorkflowIds = allWorkflowsResponse.getBody()
        .stream()
        .map(WorkflowView::getId)
        .collect(Collectors.toList());
    assertThat(deployedWorkflowIds)
        .containsExactlyInAnyOrder("valid-dummy-workflow-1", "valid-dummy-workflow-2");
  }

  @Test
  @Order(2)
  void executeWorkflowsTest() {
    // execute workflows
    engine.onEvent(messageReceived("/valid-dummy-workflow-1"));
    engine.onEvent(messageReceived("/valid-dummy-workflow-2"));
    engine.onEvent(messageReceived("/valid-dummy-workflow-2"));

    // list instances
    ResponseEntity<List<WorkflowInstView>> listResponseEntity = listInstances("valid-dummy-workflow-1");
    assertThat(listResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listResponseEntity.getBody()).hasSize(1);

    listResponseEntity = listInstances("valid-dummy-workflow-2");
    assertThat(listResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listResponseEntity.getBody()).hasSize(2);
  }

  @Test
  @Order(3)
  void listInstancesByStatusTest() throws InterruptedException {
    // list pending instances
    Thread.sleep(200);
    ResponseEntity<List<WorkflowInstView>> listResponseEntity =
        listInstances("valid-dummy-workflow-1", "pending");
    assertThat(listResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listResponseEntity.getBody()).hasSize(1);

    listResponseEntity = listInstances("valid-dummy-workflow-2", "pending");
    assertThat(listResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listResponseEntity.getBody()).isEmpty();

    // list completed instances
    listResponseEntity = listInstances("valid-dummy-workflow-1", "completed");
    assertThat(listResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listResponseEntity.getBody()).isEmpty();

    listResponseEntity = listInstances("valid-dummy-workflow-2", "completed");
    assertThat(listResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(listResponseEntity.getBody()).hasSize(2);
  }
}
