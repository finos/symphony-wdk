package com.symphony.bdk.workflow.swadl;

import com.symphony.bdk.workflow.engine.camunda.variable.BpmnToAndFromBaseActivityMixin;
import com.symphony.bdk.workflow.engine.camunda.variable.EscapedJsonVariableDeserializer;
import com.symphony.bdk.workflow.swadl.v1.Activity;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test mimics the conversion between:
 * SWADL -> Java model -> BPMN -> expression resolution -> Java model (executors)
 */
class VariableDeserializersTest {

  private ObjectMapper swadlToModelMapper;
  private ObjectMapper bpmnToModelMapper;

  @BeforeEach
  void setUp() {
    swadlToModelMapper = new ObjectMapper();
    swadlToModelMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    swadlToModelMapper.addMixIn(BaseActivity.class, SwadlToBaseActivityMixin.class);
    swadlToModelMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
    swadlToModelMapper.registerModule(new SimpleModule().addDeserializer(Activity.class, new ActivityDeserializer()));

    bpmnToModelMapper = new ObjectMapper();
    bpmnToModelMapper.addMixIn(BaseActivity.class, BpmnToAndFromBaseActivityMixin.class);
    bpmnToModelMapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
    bpmnToModelMapper.registerModule(
        new SimpleModule().addDeserializer(List.class, new EscapedJsonVariableDeserializer<>(List.class)));
    bpmnToModelMapper.registerModule(
        new SimpleModule().addDeserializer(Map.class, new EscapedJsonVariableDeserializer<>(Map.class)));
  }

  @EqualsAndHashCode(callSuper = true)
  @Data
  public static class VariableActivity extends BaseActivity {
    private List<String> listField;
    private Map<String, String> mapField;
    private Long longField;
    private Boolean boolField;
    private String stringField;
  }

  @Test
  void listOfVariables() throws JsonProcessingException {
    VariableActivity swadlModel = swadlToModelMapper.readValue("{\n"
        + "  \"id\": \"123\",\n"
        + "  \"listField\": [\n"
        + "    \"${variables.aVar}\"\n"
        + "  ]\n"
        + "}", VariableActivity.class);

    String swadlModelInBpmn = bpmnToModelMapper.writeValueAsString(swadlModel);
    assertThat(swadlModelInBpmn).doesNotContain("variableProperties")
        .containsOnlyOnce("listField")
        .containsOnlyOnce("variables");

    String resolvedExpression = swadlModelInBpmn.replace("${variables.aVar}", "ABC");
    VariableActivity modelInExecutor = bpmnToModelMapper.readValue(resolvedExpression, VariableActivity.class);

    assertThat(modelInExecutor.getListField()).isEqualTo(List.of("ABC"));
  }

  @Test
  void listAsVariable() throws JsonProcessingException {
    VariableActivity swadlModel = swadlToModelMapper.readValue("{\n"
        + "  \"id\": \"123\",\n"
        + "  \"listField\": "
        + "\"${variables.aVar}\"\n"
        + "}", VariableActivity.class);

    String swadlModelInBpmn = bpmnToModelMapper.writeValueAsString(swadlModel);
    assertThat(swadlModelInBpmn).doesNotContain("variableProperties")
        .containsOnlyOnce("listField")
        .containsOnlyOnce("variables");

    String resolvedExpression = swadlModelInBpmn.replace("${variables.aVar}", "[\\\"ABC\\\"]");
    VariableActivity modelInExecutor = bpmnToModelMapper.readValue(resolvedExpression, VariableActivity.class);

    assertThat(modelInExecutor.getListField()).isEqualTo(List.of("ABC"));
  }

  @Test
  void simpleList() throws JsonProcessingException {
    VariableActivity swadlModel = swadlToModelMapper.readValue("{\n"
        + "  \"id\": \"123\",\n"
        + "  \"listField\": [\n"
        + "    \"ABC\"\n"
        + "  ]\n"
        + "}", VariableActivity.class);

    String swadlModelInBpmn = bpmnToModelMapper.writeValueAsString(swadlModel);
    assertThat(swadlModelInBpmn).doesNotContain("variableProperties");

    VariableActivity modelInExecutor = bpmnToModelMapper.readValue(swadlModelInBpmn, VariableActivity.class);

    assertThat(modelInExecutor.getListField()).isEqualTo(List.of("ABC"));
  }

  @Test
  void mapOfVariables() throws JsonProcessingException {
    VariableActivity swadlModel = swadlToModelMapper.readValue("{\n"
        + "  \"id\": \"123\",\n"
        + "  \"mapField\": {\n"
        + "    \"entry\": \"${variables.aVar}\"\n"
        + "  }\n"
        + "}", VariableActivity.class);

    String swadlModelInBpmn = bpmnToModelMapper.writeValueAsString(swadlModel);
    assertThat(swadlModelInBpmn).doesNotContain("variableProperties")
        .containsOnlyOnce("mapField")
        .containsOnlyOnce("variables");

    String resolvedExpression = swadlModelInBpmn.replace("${variables.aVar}", "ABC");
    VariableActivity modelInExecutor = bpmnToModelMapper.readValue(resolvedExpression, VariableActivity.class);

    assertThat(modelInExecutor.getMapField()).isEqualTo(Map.of("entry", "ABC"));
  }

  @Test
  void mapAsVariable() throws JsonProcessingException {
    VariableActivity swadlModel = swadlToModelMapper.readValue("{\n"
        + "  \"id\": \"123\",\n"
        + "  \"mapField\": "
        + "\"${variables.aVar}\"\n"
        + "}", VariableActivity.class);

    String swadlModelInBpmn = bpmnToModelMapper.writeValueAsString(swadlModel);
    assertThat(swadlModelInBpmn).doesNotContain("variableProperties")
        .containsOnlyOnce("mapField")
        .containsOnlyOnce("variables");

    String resolvedExpression = swadlModelInBpmn.replace("${variables.aVar}", "{\\\"entry\\\":\\\"ABC\\\"}");
    VariableActivity modelInExecutor = bpmnToModelMapper.readValue(resolvedExpression, VariableActivity.class);

    assertThat(modelInExecutor.getMapField()).isEqualTo(Map.of("entry", "ABC"));
  }

  @Test
  void longAsVariable() throws JsonProcessingException {
    VariableActivity swadlModel = swadlToModelMapper.readValue("{\n"
        + "  \"id\": \"123\",\n"
        + "  \"longField\": "
        + "\"${variables.aVar}\"\n"
        + "}", VariableActivity.class);

    String swadlModelInBpmn = bpmnToModelMapper.writeValueAsString(swadlModel);
    assertThat(swadlModelInBpmn).doesNotContain("variableProperties")
        .containsOnlyOnce("longField")
        .containsOnlyOnce("variables");

    String resolvedExpression = swadlModelInBpmn.replace("${variables.aVar}", "123");
    VariableActivity modelInExecutor = bpmnToModelMapper.readValue(resolvedExpression, VariableActivity.class);

    assertThat(modelInExecutor.getLongField()).isEqualTo(123L);
  }

  @Test
  void longAsValue() throws JsonProcessingException {
    VariableActivity swadlModel = swadlToModelMapper.readValue("{\n"
        + "  \"id\": \"123\",\n"
        + "  \"longField\": 123\n"
        + "}", VariableActivity.class);

    String swadlModelInBpmn = bpmnToModelMapper.writeValueAsString(swadlModel);
    assertThat(swadlModelInBpmn).doesNotContain("variableProperties")
        .containsOnlyOnce("longField");

    VariableActivity modelInExecutor = bpmnToModelMapper.readValue(swadlModelInBpmn, VariableActivity.class);

    assertThat(modelInExecutor.getLongField()).isEqualTo(123L);
  }

  @Test
  void booleanAsVariable() throws JsonProcessingException {
    VariableActivity swadlModel = swadlToModelMapper.readValue("{\n"
        + "  \"id\": \"123\",\n"
        + "  \"boolField\": "
        + "\"${variables.aVar}\"\n"
        + "}", VariableActivity.class);

    String swadlModelInBpmn = bpmnToModelMapper.writeValueAsString(swadlModel);
    assertThat(swadlModelInBpmn).doesNotContain("variableProperties")
        .containsOnlyOnce("boolField")
        .containsOnlyOnce("variables");

    String resolvedExpression = swadlModelInBpmn.replace("${variables.aVar}", "true");
    VariableActivity modelInExecutor = bpmnToModelMapper.readValue(resolvedExpression, VariableActivity.class);

    assertThat(modelInExecutor.getBoolField()).isTrue();
  }

  @Test
  void booleanAsValue() throws JsonProcessingException {
    VariableActivity swadlModel = swadlToModelMapper.readValue("{\n"
        + "  \"id\": \"123\",\n"
        + "  \"boolField\": true\n"
        + "}", VariableActivity.class);

    String swadlModelInBpmn = bpmnToModelMapper.writeValueAsString(swadlModel);
    assertThat(swadlModelInBpmn).doesNotContain("variableProperties")
        .containsOnlyOnce("boolField");

    VariableActivity modelInExecutor = bpmnToModelMapper.readValue(swadlModelInBpmn, VariableActivity.class);

    assertThat(modelInExecutor.getBoolField()).isTrue();
  }

  @Test
  void stringAsVariable() throws JsonProcessingException {
    VariableActivity swadlModel = swadlToModelMapper.readValue("{\n"
        + "  \"id\": \"123\",\n"
        + "  \"stringField\": "
        + "\"${variables.aVar}\"\n"
        + "}", VariableActivity.class);

    String swadlModelInBpmn = bpmnToModelMapper.writeValueAsString(swadlModel);
    assertThat(swadlModelInBpmn).doesNotContain("variableProperties")
        .containsOnlyOnce("stringField")
        .containsOnlyOnce("variables");

    String resolvedExpression = swadlModelInBpmn.replace("${variables.aVar}", "value");
    VariableActivity modelInExecutor = bpmnToModelMapper.readValue(resolvedExpression, VariableActivity.class);

    assertThat(modelInExecutor.getStringField()).isEqualTo("value");
  }

  @Test
  void stringAsValue() throws JsonProcessingException {
    VariableActivity swadlModel = swadlToModelMapper.readValue("{\n"
        + "  \"id\": \"123\",\n"
        + "  \"stringField\": \"value\"\n"
        + "}", VariableActivity.class);

    String swadlModelInBpmn = bpmnToModelMapper.writeValueAsString(swadlModel);
    assertThat(swadlModelInBpmn).doesNotContain("variableProperties")
        .containsOnlyOnce("stringField");

    VariableActivity modelInExecutor = bpmnToModelMapper.readValue(swadlModelInBpmn, VariableActivity.class);

    assertThat(modelInExecutor.getStringField()).isEqualTo("value");
  }

}
