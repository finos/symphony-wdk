package com.symphony.bdk.workflow.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.symphony.bdk.workflow.engine.camunda.UtilityFunctionsMapper;

import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
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
  void encodeRawQueryTest() throws UnsupportedEncodingException {
    String encodedUrl = UtilityFunctionsMapper.encodeRawQuery(
        "https://www.wdk.symphony.com:8080/path1/path2?key1=value 1&key2=value@!$2&key3=value%3",
        StandardCharsets.UTF_8.toString());

    assertThat(encodedUrl).isEqualTo(
        "https://www.wdk.symphony.com:8080/path1/path2?key1%3Dvalue+1%26key2%3Dvalue%40%21%242%26key3%3Dvalue%253");
  }

  @Test
  void encodeRawQuery_noPort_noPath_Test() throws UnsupportedEncodingException {
    String encodedUrl = UtilityFunctionsMapper.encodeRawQuery(
        "https://www.wdk.symphony.com?key1=value 1&key2=value@!$2&key3=value%3");

    assertThat(encodedUrl).isEqualTo(
        "https://www.wdk.symphony.com?key1%3Dvalue+1%26key2%3Dvalue%40%21%242%26key3%3Dvalue%253");
  }

  @Test
  void encodeRawQuery_BadEncoding_Test() {
    assertThatThrownBy(() -> UtilityFunctionsMapper.encodeRawQuery(
        "https://www.wdk.symphony.com", "UTF-8_BAD_ENCODING"))
        .isInstanceOf(UnsupportedEncodingException.class);
  }

  @Test
  void decodeRawQueryTest() throws UnsupportedEncodingException {
    String encodedUrl = UtilityFunctionsMapper.decodeRawQuery(
        "https://www.wdk.symphony.com:8080/path1/path2?key1%3Dvalue+1%26key2%3Dvalue%40%21%242%26key3%3Dvalue%253");

    assertThat(encodedUrl).isEqualTo(
        "https://www.wdk.symphony.com:8080/path1/path2?key1=value 1&key2=value@!$2&key3=value%3");
  }

  @Test
  void decodeRawQuery_BadEncoding_Test() {
    assertThatThrownBy(() -> UtilityFunctionsMapper.decodeRawQuery(
        "https://www.wdk.symphony.com", "UTF-8_BAD_ENCODING"))
        .isInstanceOf(UnsupportedEncodingException.class);
  }

}
