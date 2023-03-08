package com.symphony.bdk.workflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;


class WorkflowsMgtApiControllerIntegrationTest extends IntegrationTest  {

  @Autowired
  private TestRestTemplate restTemplate;

  private static final String VALID_SWADL = "id: dummy-workflow-management-api-integration-test\n" +
          "activities:\n" +
          "  - execute-script:\n" +
          "      id: script0\n" +
          "      on:\n" +
          "        message-received:\n" +
          "          content: /dummy-workflow-management-api-integration-test\n" +
          "      script: |\n" +
          "        messageService.send(\"123\", \"OK\")";

  private static final String INVALID_SWADL = "id: invalid-workflow";
  private static final String MANAGEMENT_API_DEPLOY_URL = "http://localhost:%s/wdk/v1/management/workflows";
  private static final String MANAGEMENT_API_UPDATE_URL = "http://localhost:%s/wdk/v1/management/workflows";
  //private static final String MANAGEMENT_API_SAVE_AND_DEPLOY_URL = "http://localhost:%s/wdk/v1/management/workflows/%s";

  @Test
  void saveAndDeployTest() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.add("X-Management-Token", "MANAGEMENT_TOKEN_VALUE");

    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("swadl", VALID_SWADL);
    map.add("description", "dummy workflow");
    map.add("author", "1234");

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
    ResponseEntity<Object> response = restTemplate.postForEntity(
            String.format(MANAGEMENT_API_DEPLOY_URL, port), request, Object.class);

    engine.onEvent(messageReceived("/dummy-workflow-management-api-integration-test"));
    verify(messageService, timeout(5000)).send("123", "OK");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(response.getBody()).isNull();
  }

  @Test
  void saveAndDeployTest_invalidSwadl() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.add("X-Management-Token", "MANAGEMENT_TOKEN_VALUE");

    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("swadl", INVALID_SWADL);
    map.add("description", "dummy workflow");
    map.add("author", "1234");

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
    ResponseEntity<Object> response = restTemplate.postForEntity(
            String.format(MANAGEMENT_API_DEPLOY_URL, port), request, Object.class);
    Map<String, String> responseBody = (Map<String, String>) response.getBody();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(responseBody.get("message")).isEqualTo("SWADL content is not valid");
  }

  @Test
  void saveAndDeploy_unauthorized() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.add("X-Management-Token", "BAD_TOKEN");

    MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
    map.add("swadl", VALID_SWADL);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
    ResponseEntity<Object> response = restTemplate.postForEntity(
            String.format(MANAGEMENT_API_DEPLOY_URL, port), request , Object.class);
    Map<String, String> responseBody = (Map<String, String>) response.getBody();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(responseBody.get("message")).isEqualTo("Request is not authorised");
  }

  /*@ParameterizedTest
  //@CsvSource({MANAGEMENT_API_DEPLOY_URL, MANAGEMENT_API_UPDATE_URL, MANAGEMENT_API_SAVE_AND_DEPLOY_URL})
  void noManagementTokenHeaderTest(String url) {
    restTemplate.getRestTemplate().setInterceptors(
            Collections.singletonList((request, body, execution) -> {
              request.getHeaders()
                      .add("X-Management-Token", "MANAGEMENT_TOKEN_VALUE");
              return execution.execute(request, body);
            }));
//restTemplate.getForEntity(String.format(url, port), Object.class).getStatusCode()

    restTemplate.getForObject(String.format(url, port), Object.class);
  }*/
}
