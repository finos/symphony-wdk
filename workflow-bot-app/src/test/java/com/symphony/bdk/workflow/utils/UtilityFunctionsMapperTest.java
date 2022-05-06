package com.symphony.bdk.workflow.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphony.bdk.workflow.engine.camunda.UtilityFunctionsMapper;

import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@SuppressWarnings("unchecked")
class UtilityFunctionsMapperTest {

  @Test
  void jsonStringTest() {
    final Object json = UtilityFunctionsMapper.json("This is a regular string");
    assertThat(json).isEqualTo("This is a regular string");
  }

  @Test
  void jsonIntegerTest() {
    final Object json = UtilityFunctionsMapper.json("2");
    assertThat(json).isEqualTo(2);
  }

  @Test
  void jsonBooleanTest() {
    final Object json = UtilityFunctionsMapper.json("false");
    assertThat(json).isEqualTo(false);
  }

  @Test
  void jsonMapTest() {
    final Object json = UtilityFunctionsMapper.json("{\"key\": \"value\"}");
    final Map<String, String> map = (Map<String, String>) json;
    assertThat(map).containsEntry("key", "value");
  }

  @Test
  void jsonNestedMapTest() {
    final Object json = UtilityFunctionsMapper.json("{\"outerKey\":\n {\"innerKey\": \"value\"}}");
    final Map<String, Object> map = (Map<String, Object>) json;
    assertThat((Map<String, String>) map.get("outerKey")).containsEntry("innerKey", "value");
  }

  @Test
  void jsonEmptyTest() {
    final Object json = UtilityFunctionsMapper.json("");
    assertThat(json.toString()).isEmpty();
  }

  @Test
  void encodeQueryParametersTest() {
    String encodedUrl = UtilityFunctionsMapper.encodeQueryParameters(
        "https://www.wdk.symphony.com:8080/path1/path2?key1=value 1&key2=value@!$2&key3=value%3");

    assertThat(encodedUrl).as("The parameters have been encoded").isEqualTo(
        "https://www.wdk.symphony.com:8080/path1/path2?key1=value+1&key2=value%40%21%242&key3=value%253");
  }

  @Test
  void encodeQueryParameters_alreadyEncoded_Test() {
    String alreadyEncoded =
        "https://www.wdk.symphony.com:8080/path1/path2?key1=value+1&key2=value%40%21%242&key3=value%253";
    String encodedUrl = UtilityFunctionsMapper.encodeQueryParameters(alreadyEncoded);

    assertThat(encodedUrl).as("The parameters were already encoded").isEqualTo(alreadyEncoded);
  }

  @Test
  void encodeQueryParameters_alreadyEncoded_and_decoded_Test() {
    String fullUrl =
        "https://www.wdk.symphony.com:8080/path1/path2?key1=value+1&key2=value@!$2&key3=value%253";
    String encodedUrl = UtilityFunctionsMapper.encodeQueryParameters(fullUrl);

    MultiValueMap<String, String> queryParamsMap =
        UriComponentsBuilder.fromUriString(encodedUrl).build().getQueryParams();

    // The order of parameters will not be preserved
    assertThat(queryParamsMap.size()).isEqualTo(3);
    assertThat(queryParamsMap.get("key1").get(0)).as("The parameter value is already encoded").isEqualTo("value+1");
    assertThat(queryParamsMap.get("key2").get(0)).as("The parameter value was not already encoded")
        .isEqualTo("value%40%21%242");
    assertThat(queryParamsMap.get("key3").get(0)).as("The parameter value is already encoded").isEqualTo("value%253");
  }

  @Test
  void encodeQueryParameters_noParameters_Test() {
    String fullUrl = "https://www.wdk.symphony.com:8080/path1/path2";
    String encodedUrl = UtilityFunctionsMapper.encodeQueryParameters(fullUrl);

    assertThat(encodedUrl).isEqualTo(fullUrl);
  }

}
