package com.symphony.bdk.workflow.swadl;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphony.bdk.workflow.engine.camunda.variable.EscapedJsonVariableDeserializer;
import com.symphony.bdk.workflow.swadl.v1.Variable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
    swadlToModelMapper.registerModule(
        new SimpleModule().addDeserializer(Variable.class, new VariableListDeserializer()));

    bpmnToModelMapper = new ObjectMapper();
    bpmnToModelMapper.registerModule(
        new SimpleModule().addDeserializer(Variable.class, new EscapedJsonVariableDeserializer()));
  }

  @Test
  void variableToList() throws JsonProcessingException {
    Variable<List<String>> swadlModel = swadlToModelMapper.readValue("\"${variables.aVar}\"", new TypeReference<>() {});
    String swadlModelInBpmn = bpmnToModelMapper.writeValueAsString(swadlModel);
    String resolvedExpression = swadlModelInBpmn.replace("${variables.aVar}", "[\\\"ABC\\\"]");
    Variable<List<String>> modelInExecutor = bpmnToModelMapper.readValue(resolvedExpression, new TypeReference<>() {});

    assertThat(modelInExecutor.get()).isEqualTo(List.of("ABC"));
  }

  @Test
  void listToList() throws JsonProcessingException {
    Variable<List<String>> swadlModel = swadlToModelMapper.readValue("[\"ABC\"]", new TypeReference<>() {});
    String swadlModelInBpmn = bpmnToModelMapper.writeValueAsString(swadlModel);
    String resolvedExpression = swadlModelInBpmn;
    Variable<List<String>> modelInExecutor = bpmnToModelMapper.readValue(resolvedExpression, new TypeReference<>() {});

    assertThat(modelInExecutor.get()).isEqualTo(List.of("ABC"));
  }

  @Test
  void variableToMap() throws JsonProcessingException {
    Variable<Map<String, String>> swadlModel =
        swadlToModelMapper.readValue("\"${variables.aVar}\"", new TypeReference<>() {});
    String swadlModelInBpmn = bpmnToModelMapper.writeValueAsString(swadlModel);
    String resolvedExpression = swadlModelInBpmn.replace("${variables.aVar}", "{\\\"key\\\":\\\"value\\\"}");
    Variable<Map<String, String>> modelInExecutor =
        bpmnToModelMapper.readValue(resolvedExpression, new TypeReference<>() {});

    assertThat(modelInExecutor.get()).isEqualTo(Map.of("key", "value"));
  }

  @Test
  void mapToMap() throws JsonProcessingException {
    Variable<Map<String, String>> swadlModel =
        swadlToModelMapper.readValue("{\"key\":\"value\"}", new TypeReference<>() {});
    String swadlModelInBpmn = bpmnToModelMapper.writeValueAsString(swadlModel);
    String resolvedExpression = swadlModelInBpmn;
    Variable<Map<String, String>> modelInExecutor =
        bpmnToModelMapper.readValue(resolvedExpression, new TypeReference<>() {});

    assertThat(modelInExecutor.get()).isEqualTo(Map.of("key", "value"));
  }

  @Test
  void variableToNumber() throws JsonProcessingException {
    Variable<Number> swadlModel = swadlToModelMapper.readValue("\"${variables.aVar}\"", new TypeReference<>() {});
    String swadlModelInBpmn = bpmnToModelMapper.writeValueAsString(swadlModel);
    String resolvedExpression = swadlModelInBpmn.replace("${variables.aVar}", "123");
    Variable<Number> modelInExecutor = bpmnToModelMapper.readValue(resolvedExpression, new TypeReference<>() {});

    assertThat(modelInExecutor.get()).isEqualTo(123);
  }

  @Test
  void numberToNumber() throws JsonProcessingException {
    Variable<Number> swadlModel = swadlToModelMapper.readValue("123", new TypeReference<>() {});
    String swadlModelInBpmn = bpmnToModelMapper.writeValueAsString(swadlModel);
    String resolvedExpression = swadlModelInBpmn;
    Variable<Number> modelInExecutor = bpmnToModelMapper.readValue(resolvedExpression, new TypeReference<>() {});

    assertThat(modelInExecutor.get()).isEqualTo(123);
  }

  @Test
  void variableToBoolean() throws JsonProcessingException {
    Variable<Boolean> swadlModel = swadlToModelMapper.readValue("\"${variables.aVar}\"", new TypeReference<>() {});
    String swadlModelInBpmn = bpmnToModelMapper.writeValueAsString(swadlModel);
    String resolvedExpression = swadlModelInBpmn.replace("${variables.aVar}", "true");
    Variable<Boolean> modelInExecutor = bpmnToModelMapper.readValue(resolvedExpression, new TypeReference<>() {});

    assertThat(modelInExecutor.get()).isTrue();
  }

  @Test
  void booleanToBoolean() throws JsonProcessingException {
    Variable<Boolean> swadlModel = swadlToModelMapper.readValue("true", new TypeReference<>() {});
    String swadlModelInBpmn = bpmnToModelMapper.writeValueAsString(swadlModel);
    String resolvedExpression = swadlModelInBpmn;
    Variable<Boolean> modelInExecutor = bpmnToModelMapper.readValue(resolvedExpression, new TypeReference<>() {});

    assertThat(modelInExecutor.get()).isTrue();
  }

  @Test
  void variableToString() throws JsonProcessingException {
    Variable<String> swadlModel = swadlToModelMapper.readValue("\"${variables.aVar}\"", new TypeReference<>() {});
    String swadlModelInBpmn = bpmnToModelMapper.writeValueAsString(swadlModel);
    String resolvedExpression = swadlModelInBpmn.replace("${variables.aVar}", "val");
    Variable<String> modelInExecutor = bpmnToModelMapper.readValue(resolvedExpression, new TypeReference<>() {});

    assertThat(modelInExecutor.get()).isEqualTo("val");
  }

  @Test
  void stringToString() throws JsonProcessingException {
    Variable<String> swadlModel = swadlToModelMapper.readValue("\"val\"", new TypeReference<>() {});
    String swadlModelInBpmn = bpmnToModelMapper.writeValueAsString(swadlModel);
    String resolvedExpression = swadlModelInBpmn;
    Variable<String> modelInExecutor = bpmnToModelMapper.readValue(resolvedExpression, new TypeReference<>() {});

    assertThat(modelInExecutor.get()).isEqualTo("val");
  }
}
