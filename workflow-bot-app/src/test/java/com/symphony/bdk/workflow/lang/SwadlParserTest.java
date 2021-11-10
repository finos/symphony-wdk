package com.symphony.bdk.workflow.lang;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.exception.SwadlNotValidException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.Test;

import java.io.IOException;


class SwadlParserTest {

  @Test
  void shouldLoadGlobalVariablesWhenLoadingValidSwadl()
      throws IOException, ProcessingException {
    Workflow workflow = SwadlParser.fromYaml(getClass()
        .getResourceAsStream("valid_swadl.yaml"));

    assertThat(workflow.getVariables()).isNotNull();
    assertThat(workflow.getVariables()).isNotEmpty();
    assertThat(workflow.getVariables().size()).isEqualTo(2);
    assertThat(workflow.getVariables().get("var1")).isEqualTo("variable1");
    assertThat(workflow.getVariables().get("var2")).isEqualTo("variable2");
  }

  @Test
  void shouldIgnoreVariablesWhenLoadingInvalidSwadl()
      throws IOException, ProcessingException {
    Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream("invalid_swadl.swadl.yaml"));

    assertThat(workflow.getVariables()).isEmpty();
  }

  @Test
  void customActivity() throws IOException, ProcessingException {
    Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream("custom-activity.swadl.yaml"));

    assertThat(workflow.getFirstActivity()).hasValueSatisfying(c -> {
      assertThat(c.getActivity().getVariableProperties().get("my-parameter")).isEqualTo("abc");
    });
  }

  @Test
  void customActivity_notFound() {
    assertThatThrownBy(
        () -> SwadlParser.fromYaml(getClass().getResourceAsStream("custom-activity-not-found.swadl.yaml")))
        .describedAs("Should fail are validation time because the JSON schema is updated on the fly")
        .isInstanceOf(SwadlNotValidException.class);
  }

  @Test
  void customActivity_duplicateDefinition() {
    assertThatThrownBy(() -> SwadlParser.fromYaml(
        getClass().getResourceAsStream("custom-activity-duplicate-definition.swadl.yaml")))
        .describedAs(
            "Workflow is invalid because 2 DuplicateCustomActivity classes are defined (in different packages")
        .isInstanceOf(JsonMappingException.class);
  }



}
