package com.symphony.bdk.workflow.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.service.session.SessionService;
import com.symphony.bdk.gen.api.model.UserV2;
import com.symphony.bdk.workflow.engine.camunda.UtilityFunctionsMapper;

import org.junit.jupiter.api.Test;

import java.util.Map;

@SuppressWarnings("unchecked")
class UtilityFunctionsMapperTest {

  private static final String BOT_NAME = "BOT NAME";
  private static final Long BOT_UID = 1234L;
  private final SessionService sessionServiceMock = mock(SessionService.class);

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
  void sessionTest() {
    UtilityFunctionsMapper utilityFunctionsMapper = new UtilityFunctionsMapper(this.sessionServiceMock);
    UserV2 userV2 = new UserV2().id(BOT_UID).displayName(BOT_NAME);
    when(this.sessionServiceMock.getSession()).thenReturn(userV2);

    UserV2 actual = utilityFunctionsMapper.session();

    assertThat(actual.getDisplayName()).isEqualTo(BOT_NAME);
    assertThat(actual.getId()).isEqualTo(BOT_UID);
  }
}
