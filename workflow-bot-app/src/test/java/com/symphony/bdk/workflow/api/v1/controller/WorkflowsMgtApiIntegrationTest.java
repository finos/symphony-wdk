package com.symphony.bdk.workflow.api.v1.controller;

import com.symphony.bdk.workflow.IntegrationTest;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WorkflowsMgtApiIntegrationTest extends IntegrationTest {

  @Autowired
  private TestRestTemplate restTemplate;

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

  private static final String MANAGEMENT_TOKEN_KEY = "X-Management-Token";
  private static final String MANAGEMENT_TOKEN_VALUE = "MANAGEMENT_TOKEN_VALUE";
  private static final String MONITORING_TOKEN_KEY = "X-Monitoring-Token";
  private static final String MONITORING_TOKEN_VALUE = "MONITORING_TOKEN_VALUE";
  private static final String CONTENT_TYPE_KEY = "Content-Type";
  private static final String VALIDATE_AND_DEPLOY_URL = "http://localhost:%s/wdk/v1/management/workflows";
  private static final String VALIDATE_AND_UPDATE_URL = "http://localhost:%s/wdk/v1/management/workflows";

  private static final String SET_ACTIVE_VERSION_URL = "http://localhost:%s/wdk/v1/management/workflows/%s/versions/%s";
  private static final String GET_SWADL_BY_ID_URL = "http://localhost:%s/wdk/v1/management/workflows/%s";
  private static final String DELETE_SWADL_BY_ID_URL =
          "http://localhost:%s/wdk/v1/management/workflows/%s";
  private static final String DELETE_SWADL_BY_ID_AND_VERSION_URL =
          "http://localhost:%s/wdk/v1/management/workflows/%s/versions/%s";
  private static final String LIST_ALL_DEPLOYED_WORKFLOWS_URL = "http://localhost:%s/wdk/v1/workflows/";

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

  private ResponseEntity<List<Pair<String, Boolean>>> getSwadlById(String workflowId) {
    restTemplate.getRestTemplate().setInterceptors(
            Collections.singletonList((request, body, execution) -> {
              request.getHeaders().add(MANAGEMENT_TOKEN_KEY, MANAGEMENT_TOKEN_VALUE);
              request.getHeaders().add(CONTENT_TYPE_KEY, "text/plain");
              return execution.execute(request, body);
            }));

    ResponseEntity<Object[]> response = restTemplate.getForEntity(
            String.format(GET_SWADL_BY_ID_URL, port, workflowId), Object[].class);

    List<Pair<String, Boolean>> responseBodyAsList = new ArrayList<>();

    if (response.getBody() != null) {
      Arrays.stream(response.getBody()).forEach(e ->
              responseBodyAsList.add(Pair.of((String) ((Map) e).get("swadl"), (Boolean) ((Map) e).get("active")))
      );
    }

    return new ResponseEntity<>(responseBodyAsList, response.getStatusCode());
  }

  private ResponseEntity<Void> saveAndDeploy(String swadl, String description) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.add(MANAGEMENT_TOKEN_KEY, MANAGEMENT_TOKEN_VALUE);

    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("swadl", swadl);
    map.add("description", description);

    return restTemplate.postForEntity(
            String.format(VALIDATE_AND_DEPLOY_URL, port), new HttpEntity<>(map, headers), Void.class);
  }

  private ResponseEntity<Void> validateAndUpdate(String swadl) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.add(MANAGEMENT_TOKEN_KEY, MANAGEMENT_TOKEN_VALUE);

    MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
    requestBody.add("swadl", swadl);

    return restTemplate.exchange(String.format(VALIDATE_AND_UPDATE_URL, port),
            HttpMethod.PUT, new HttpEntity<>(requestBody, headers), Void.class, Collections.emptyList());
  }

  private ResponseEntity<Void> rollBackWorkflowVersion(String workflowId, Long versionToRollback) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);
    headers.add(MANAGEMENT_TOKEN_KEY, MANAGEMENT_TOKEN_VALUE);

    return restTemplate.postForEntity(
            String.format(SET_ACTIVE_VERSION_URL, port, workflowId, versionToRollback),
            new HttpEntity<>(headers), Void.class);
  }

  private ResponseEntity<Void> deleteWorkflow(String workflowId) {
    return deleteWorkflow(workflowId, null);
  }

  private ResponseEntity<Void> deleteWorkflow(String workflowId, Long version) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);
    headers.add(MANAGEMENT_TOKEN_KEY, MANAGEMENT_TOKEN_VALUE);

    if (version == null) {
      return restTemplate.exchange(String.format(DELETE_SWADL_BY_ID_URL, port, workflowId),
              HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
    } else {
      return restTemplate.exchange(String.format(DELETE_SWADL_BY_ID_AND_VERSION_URL, port, workflowId, version),
              HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
    }
  }

  private ResponseEntity<List<WorkflowView>> listAllWorkflows() {
    restTemplate.getRestTemplate().setInterceptors(
            Collections.singletonList((request, body, execution) -> {
              request.getHeaders().add(MONITORING_TOKEN_KEY, MONITORING_TOKEN_VALUE);
              request.getHeaders().add(CONTENT_TYPE_KEY, "application/json");
              return execution.execute(request, body);
            }));

    ResponseEntity<WorkflowView[]> response = restTemplate.getForEntity(
            String.format(LIST_ALL_DEPLOYED_WORKFLOWS_URL, port), WorkflowView[].class);

    if (response.getBody() != null) {
      return new ResponseEntity<>(Arrays.asList(response.getBody()), response.getStatusCode());
    } else {
      return new ResponseEntity<>(Collections.emptyList(), response.getStatusCode());
    }
  }
}
