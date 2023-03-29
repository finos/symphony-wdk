package com.symphony.bdk.workflow.api.v1.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressFBWarnings(value = {"NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD"},
    justification = "A NP should make the test fail")
class WorkflowsMgtApiIntegrationTest extends ApiIntegrationTest {
  private static final String VALID_SWADL_V1 = "id: dummy-workflow\n"
      + "properties: \n"
      + "  publish: false\n"
      + "activities:\n"
      + "  - execute-script:\n"
      + "      id: script0\n"
      + "      on:\n"
      + "        message-received:\n"
      + "          content: /dummy-workflow\n"
      + "      script: |\n"
      + "        messageService.send(\"123\", \"OK from V1\")";

  private static final String VALID_SWADL_V2 = "id: dummy-workflow\n"
      + "activities:\n"
      + "  - execute-script:\n"
      + "      id: v2Script0\n"
      + "      on:\n"
      + "        message-received:\n"
      + "          content: /dummy-workflow\n"
      + "      script: |\n"
      + "        messageService.send(\"123\", \"OK from V2\")";

  private static final String UPDATED_VALID_SWADL_V1 = "id: dummy-workflow\n"
      + "properties: \n"
      + "  publish: true\n"
      + "activities:\n"
      + "  - execute-script:\n"
      + "      id: updated\n"
      + "      on:\n"
      + "        message-received:\n"
      + "          content: /dummy-workflow\n"
      + "      script: |\n"
      + "        messageService.send(\"123\", \"OK from V1\")";

  private static Long versionToRollback = -1L;

  @Test
  @Order(1)
  void saveDraftWorkflowTest() {
    // save a draft workflow
    ResponseEntity<Void> voidResponse = saveAndDeploy(VALID_SWADL_V1, "dummy-workflow");
    assertThat(voidResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(voidResponse.getBody()).isNull();

    // request the workflow swadl
    ResponseEntity<List<Pair<String, Boolean>>> getSwadlResponse = getSwadlById("dummy-workflow", true);
    assertThat(getSwadlResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getSwadlResponse.getBody()).hasSize(1);
    assertThat(getSwadlResponse.getBody().get(0)).isEqualTo(Pair.of(VALID_SWADL_V1, false));
  }

  @Test
  @Order(2)
  void updateWorkflowTest() {
    // update and deploy the workflow
    ResponseEntity<Void> updateResponse = validateAndUpdate(UPDATED_VALID_SWADL_V1);
    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(updateResponse.getBody()).isNull();

    // request the workflow updated swadl
    ResponseEntity<List<Pair<String, Boolean>>> getSwadlResponse = getSwadlById("dummy-workflow");
    assertThat(getSwadlResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getSwadlResponse.getBody()).hasSize(1);
    assertThat(getSwadlResponse.getBody().get(0)).isEqualTo(Pair.of(UPDATED_VALID_SWADL_V1, true));
  }

  @Test
  @Order(3)
  void deployWorkflowNewVersionTest() {
    // store the workflow version id
    ResponseEntity<List<WorkflowView>> allWorkflows = listAllWorkflows();
    assertThat(allWorkflows.getBody()).hasSize(1);
    versionToRollback = allWorkflows.getBody().get(0).getVersion();

    // save a new version of the workflow
    ResponseEntity<Void> voidResponse = saveAndDeploy(VALID_SWADL_V2, "dummy workflow v2");
    assertThat(voidResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(voidResponse.getBody()).isNull();

    // get all workflow swadl versions
    ResponseEntity<List<Pair<String, Boolean>>> getSwadlResponse = getSwadlById("dummy-workflow", true);
    assertThat(getSwadlResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getSwadlResponse.getBody())
        .containsExactlyInAnyOrder(Pair.of(UPDATED_VALID_SWADL_V1, false), Pair.of(VALID_SWADL_V2, true));

    // get active workflow swadl version
    getSwadlResponse = getSwadlById("dummy-workflow");
    assertThat(getSwadlResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getSwadlResponse.getBody()).containsOnly(Pair.of(VALID_SWADL_V2, true));
  }

  @Test
  @Order(4)
  void rollbackWorkflowVersionTest() {
    // rollback the workflow to previous version
    ResponseEntity<Void> voidResponse = rollBackWorkflowVersion("dummy-workflow", versionToRollback);
    assertThat(voidResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(voidResponse.getBody()).isNull();

    // get all workflow swadl versions
    ResponseEntity<List<Pair<String, Boolean>>> getSwadlResponse =
        getSwadlById("dummy-workflow", true);
    assertThat(getSwadlResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getSwadlResponse.getBody()).containsExactlyInAnyOrder(
        Pair.of(UPDATED_VALID_SWADL_V1, true), Pair.of(VALID_SWADL_V2, false));

    // get active workflow swadl version
    getSwadlResponse = getSwadlById("dummy-workflow");
    assertThat(getSwadlResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getSwadlResponse.getBody()).containsOnly(Pair.of(UPDATED_VALID_SWADL_V1, true));
  }

  @Test
  @Order(5)
  void deleteWorkflowVersionTest() {
    // delete workflow active version
    ResponseEntity<Void> voidResponse = deleteWorkflow("dummy-workflow", versionToRollback);
    assertThat(voidResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(voidResponse.getBody()).isNull();

    // get all workflow swadl versions
    ResponseEntity<List<Pair<String, Boolean>>> getSwadlResponse = getSwadlById("dummy-workflow", true);
    assertThat(getSwadlResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getSwadlResponse.getBody()).containsOnly(Pair.of(VALID_SWADL_V2, false));
  }

  @Test
  @Order(6)
  void deleteWorkflowTest() {
    // delete all workflow versions
    ResponseEntity<Void> voidResponse = deleteWorkflow("dummy-workflow");
    assertThat(voidResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(voidResponse.getBody()).isNull();

    // get all workflow swadl versions
    ResponseEntity<List<Pair<String, Boolean>>> getSwadlResponse = getSwadlById("dummy-workflow", true);
    assertThat(getSwadlResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getSwadlResponse.getBody()).isEmpty();
  }
}
