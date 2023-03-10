package com.symphony.bdk.workflow.api.v1.controller;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowsMgtApiIntegrationTest extends ApiIntegrationTest {
  private static final String VALID_SWADL_V1 = "id: dummy-workflow\n" +
          "properties: \n" +
          "  publish: false\n" +
          "activities:\n" +
          "  - execute-script:\n" +
          "      id: script0\n" +
          "      on:\n" +
          "        message-received:\n" +
          "          content: /dummy-workflow\n" +
          "      script: |\n" +
          "        messageService.send(\"123\", \"OK from V1\")";

  private static final String VALID_SWADL_V2 = "id: dummy-workflow\n" +
          "activities:\n" +
          "  - execute-script:\n" +
          "      id: v2Script0\n" +
          "      on:\n" +
          "        message-received:\n" +
          "          content: /dummy-workflow\n" +
          "      script: |\n" +
          "        messageService.send(\"123\", \"OK from V2\")";

  private static final String UPDATED_VALID_SWADL_V1 = "id: dummy-workflow\n" +
          "properties: \n" +
          "  publish: true\n" +
          "activities:\n" +
          "  - execute-script:\n" +
          "      id: updated\n" +
          "      on:\n" +
          "        message-received:\n" +
          "          content: /dummy-workflow\n" +
          "      script: |\n" +
          "        messageService.send(\"123\", \"OK from V1\")";

  @Test
  void updateAndRollbackWorkflowIntegrationTest() {
    // save a draft workflow
    ResponseEntity<Void> voidResponse = saveAndDeploy(VALID_SWADL_V1, "dummy-workflow");
    assertThat(voidResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(voidResponse.getBody()).isNull();

    // request the workflow swadl
    ResponseEntity<List<Pair<String, Boolean>>> getSwadlResponse = getSwadlById("dummy-workflow");
    assertThat(getSwadlResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getSwadlResponse.getBody()).hasSize(1);
    assertThat(getSwadlResponse.getBody().get(0)).isEqualTo(Pair.of(VALID_SWADL_V1, false));

    // update and deploy the workflow
    ResponseEntity<Void> updateResponse = validateAndUpdate(UPDATED_VALID_SWADL_V1);
    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(updateResponse.getBody()).isNull();

    // request the workflow updated swadl
    getSwadlResponse = getSwadlById("dummy-workflow");
    assertThat(getSwadlResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getSwadlResponse.getBody()).hasSize(1);
    assertThat(getSwadlResponse.getBody().get(0)).isEqualTo(Pair.of(UPDATED_VALID_SWADL_V1, true));

    // store the workflow version id
    ResponseEntity<List<WorkflowView>> allWorkflows = listAllWorkflows();
    assertThat(allWorkflows.getBody()).hasSize(1);
    final Long versionToRollback = allWorkflows.getBody().get(0).getVersion();

    // save a new version of the workflow
    voidResponse = saveAndDeploy(VALID_SWADL_V2, "dummy workflow v2");
    assertThat(voidResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(voidResponse.getBody()).isNull();

    // get workflow swadls
    getSwadlResponse = getSwadlById("dummy-workflow");
    assertThat(getSwadlResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getSwadlResponse.getBody())
            .containsExactlyInAnyOrder(Pair.of(UPDATED_VALID_SWADL_V1, false), Pair.of(VALID_SWADL_V2, true));

    // rollback the workflow to previous version
    voidResponse = rollBackWorkflowVersion("dummy-workflow", versionToRollback);
    assertThat(voidResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(voidResponse.getBody()).isNull();

    // get workflow swadls
    getSwadlResponse = getSwadlById("dummy-workflow");
    assertThat(getSwadlResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getSwadlResponse.getBody()).containsExactlyInAnyOrder(
            Pair.of(UPDATED_VALID_SWADL_V1, true), Pair.of(VALID_SWADL_V2, false));

    // delete workflow active version
    voidResponse = deleteWorkflow("dummy-workflow", versionToRollback);
    assertThat(voidResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(voidResponse.getBody()).isNull();

    // get workflow swadls
    getSwadlResponse = getSwadlById("dummy-workflow");
    assertThat(getSwadlResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getSwadlResponse.getBody()).containsExactlyInAnyOrder(Pair.of(VALID_SWADL_V2, false));

    // delete all workflow versions
    voidResponse = deleteWorkflow("dummy-workflow");
    assertThat(voidResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(voidResponse.getBody()).isNull();

    // get workflow swadls
    getSwadlResponse = getSwadlById("dummy-workflow");
    assertThat(getSwadlResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getSwadlResponse.getBody()).isEmpty();
  }
}
