package com.symphony.bdk.workflow.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphony.bdk.workflow.engine.executor.request.ExecuteRequestUtils;

import org.junit.jupiter.api.Test;

class ExecuteRequestUtilsTest {

  @Test
  void encodeQueryParametersTest() {
    String encodedUrl = ExecuteRequestUtils.encodeQueryParameters(
        "https://www.wdk.symphony.com:8080/path1/path2?key1=value 1&key2=value@!$2&key3=value%3");

    assertThat(encodedUrl).as("The parameters have been encoded").isEqualTo(
        "https://www.wdk.symphony.com:8080/path1/path2?key1=value+1&key2=value%40%21%242&key3=value%253");
  }


  @Test
  void encodeQueryParameters_sameParametersKeyTest() {
    String encodedUrl = ExecuteRequestUtils.encodeQueryParameters(
        "https://www.wdk.symphony.com:8080/path1/path2?key1=value 1&key1=value 2");

    assertThat(encodedUrl).as("The parameters have been encoded").isEqualTo(
        "https://www.wdk.symphony.com:8080/path1/path2?key1=value+1&key1=value+2");
  }

  @Test
  void encodeQueryParameters_noParameters_Test() {
    String fullUrl = "https://www.wdk.symphony.com:8080/path1/path2";
    String encodedUrl = ExecuteRequestUtils.encodeQueryParameters(fullUrl);

    assertThat(encodedUrl).isEqualTo(fullUrl);
  }
}
