package com.symphony.bdk.workflow.utils;

import com.symphony.bdk.core.service.session.SessionService;
import com.symphony.bdk.gen.api.model.UserV2;
import com.symphony.bdk.workflow.engine.camunda.UtilityFunctionsMapper;
import com.symphony.bdk.workflow.engine.executor.SharedDataStore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class UtilityFunctionsMapperTest {

  private static final String BOT_NAME = "BOT NAME";
  private static final Long BOT_UID = 1234L;
  private final SessionService sessionService = mock(SessionService.class);
  private final SharedDataStore sharedDataStore = mock(SharedDataStore.class);

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
    UtilityFunctionsMapper.setStaticSessionService(sessionService);
    UserV2 userV2 = new UserV2().id(BOT_UID).displayName(BOT_NAME);
    when(this.sessionService.getSession()).thenReturn(userV2);

    UserV2 actual = UtilityFunctionsMapper.session();

    assertThat(actual.getDisplayName()).isEqualTo(BOT_NAME);
    assertThat(actual.getId()).isEqualTo(BOT_UID);
  }

  @Test
  @DisplayName("Read shared data method test")
  void readSharedTest() {
    UtilityFunctionsMapper.setSharedStateService(sharedDataStore);
    when(sharedDataStore.getNamespaceData(anyString())).thenReturn(Map.of("key", "value"));
    Object actual = UtilityFunctionsMapper.readShared("namespace", "key");
    assertThat(actual).isEqualTo("value");
  }

  @Test
  @DisplayName("Write shared data method test")
  void writeSharedTest() {
    UtilityFunctionsMapper.setSharedStateService(sharedDataStore);
    doNothing().when(sharedDataStore).putNamespaceData(eq("namespace"), eq("key"), eq("value"));
    UtilityFunctionsMapper.writeShared("namespace", "key", "value");
    verify(sharedDataStore).putNamespaceData(eq("namespace"), eq("key"), eq("value"));
  }
}
