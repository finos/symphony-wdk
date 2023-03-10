package com.symphony.bdk.workflow.api.v1.controller;

import com.symphony.bdk.workflow.IntegrationTest;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ApiIntegrationTest extends IntegrationTest {
  protected static final String MANAGEMENT_TOKEN_KEY = "X-Management-Token";
  protected static final String MANAGEMENT_TOKEN_VALUE = "MANAGEMENT_TOKEN_VALUE";
  protected static final String MONITORING_TOKEN_KEY = "X-Monitoring-Token";
  protected static final String MONITORING_TOKEN_VALUE = "MONITORING_TOKEN_VALUE";
  protected static final String CONTENT_TYPE_KEY = "Content-Type";
  protected static final String VALIDATE_AND_DEPLOY_URL = "http://localhost:%s/wdk/v1/management/workflows";
  protected static final String VALIDATE_AND_UPDATE_URL = "http://localhost:%s/wdk/v1/management/workflows";
  protected static final String GET_SWADL_BY_ID_URL = "http://localhost:%s/wdk/v1/management/workflows/%s";
  protected static final String SET_ACTIVE_VERSION_URL =
          "http://localhost:%s/wdk/v1/management/workflows/%s/versions/%s";
  protected static final String DELETE_SWADL_BY_ID_AND_VERSION_URL =
          "http://localhost:%s/wdk/v1/management/workflows/%s/versions/%s";
  protected static final String DELETE_SWADL_BY_ID_URL =
          "http://localhost:%s/wdk/v1/management/workflows/%s";
  protected static final String LIST_ALL_DEPLOYED_WORKFLOWS_URL = "http://localhost:%s/wdk/v1/workflows/";
  protected static final String LIST_INSTANCES_URL = "http://localhost:%s/wdk/v1/workflows/%s/instances";
  protected static final String LIST_INSTANCES_STATUS_URL =
          "http://localhost:%s/wdk/v1/workflows/%s/instances?status=%s";

  @Autowired
  protected TestRestTemplate restTemplate;

  protected ResponseEntity<List<Pair<String, Boolean>>> getSwadlById(String workflowId) {
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

  protected ResponseEntity<Void> saveAndDeploy(String swadl, String description) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.add(MANAGEMENT_TOKEN_KEY, MANAGEMENT_TOKEN_VALUE);

    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("swadl", swadl);
    map.add("description", description);

    return restTemplate.postForEntity(
            String.format(VALIDATE_AND_DEPLOY_URL, port), new HttpEntity<>(map, headers), Void.class);
  }

  protected ResponseEntity<Void> validateAndUpdate(String swadl) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.add(MANAGEMENT_TOKEN_KEY, MANAGEMENT_TOKEN_VALUE);

    MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
    requestBody.add("swadl", swadl);

    return restTemplate.exchange(String.format(VALIDATE_AND_UPDATE_URL, port),
            HttpMethod.PUT, new HttpEntity<>(requestBody, headers), Void.class, Collections.emptyList());
  }

  protected ResponseEntity<Void> rollBackWorkflowVersion(String workflowId, Long versionToRollback) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);
    headers.add(MANAGEMENT_TOKEN_KEY, MANAGEMENT_TOKEN_VALUE);

    return restTemplate.postForEntity(
            String.format(SET_ACTIVE_VERSION_URL, port, workflowId, versionToRollback),
            new HttpEntity<>(headers), Void.class);
  }

  protected ResponseEntity<Void> deleteWorkflow(String workflowId) {
    return deleteWorkflow(workflowId, null);
  }

  protected ResponseEntity<Void> deleteWorkflow(String workflowId, Long version) {
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

  protected ResponseEntity<List<WorkflowView>> listAllWorkflows() {
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

  protected ResponseEntity<List<WorkflowInstView>> listInstances(String workflowId) {
    return listInstances(workflowId, null);
  }
  protected ResponseEntity<List<WorkflowInstView>> listInstances(String workflowId, String status) {
    restTemplate.getRestTemplate().setInterceptors(
            Collections.singletonList((request, body, execution) -> {
              request.getHeaders().add(MONITORING_TOKEN_KEY, MONITORING_TOKEN_VALUE);
              request.getHeaders().add(CONTENT_TYPE_KEY, "application/json");
              return execution.execute(request, body);
            }));

    ResponseEntity<WorkflowInstView[]> response;
    if (StringUtils.isBlank(status)) {
       response = restTemplate.getForEntity(
              String.format(LIST_INSTANCES_URL, port, workflowId), WorkflowInstView[].class, "");
    } else {
      response = restTemplate.getForEntity(String.format(LIST_INSTANCES_STATUS_URL, port, workflowId, status),
              WorkflowInstView[].class, "");
    }

    if (response.getBody() != null) {
      return new ResponseEntity<>(Arrays.asList(response.getBody()), response.getStatusCode());
    } else {
      return new ResponseEntity<>(Collections.emptyList(), response.getStatusCode());
    }
  }
}
