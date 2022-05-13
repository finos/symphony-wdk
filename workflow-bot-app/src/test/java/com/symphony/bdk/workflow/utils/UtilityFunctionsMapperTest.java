package com.symphony.bdk.workflow.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphony.bdk.workflow.engine.camunda.UtilityFunctionsMapper;

import org.junit.jupiter.api.Test;

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
}
