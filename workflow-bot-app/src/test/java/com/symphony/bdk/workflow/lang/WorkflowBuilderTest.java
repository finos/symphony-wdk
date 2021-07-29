package com.symphony.bdk.workflow.lang;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.symphony.bdk.workflow.DoSomething;
import com.symphony.bdk.workflow.swadl.WorkflowBuilder;
import com.symphony.bdk.workflow.swadl.exception.YamlNotValidException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.Test;

import java.io.IOException;


class WorkflowBuilderTest {

  @Test
  void shouldLoadGlobalVariablesWhenLoadingValidSwadl()
      throws IOException, ProcessingException {
    Workflow workflow = WorkflowBuilder.fromYaml(getClass()
        .getResourceAsStream("/workflowbuilder/valid_swadl.yaml"));

    assertThat(workflow.getVariables()).isNotNull();
    assertThat(workflow.getVariables()).isNotEmpty();
    assertThat(workflow.getVariables().size()).isEqualTo(2);
    assertThat(workflow.getVariables().get("var1")).isEqualTo("variable1");
    assertThat(workflow.getVariables().get("var2")).isEqualTo("variable2");
  }

  @Test
  void shouldIgnoreVariablesWhenLoadingInvalidSwadl()
      throws IOException, ProcessingException {
    Workflow workflow = WorkflowBuilder.fromYaml(getClass()
        .getResourceAsStream("/workflowbuilder/invalid_swadl.yaml"));

    assertThat(workflow.getVariables()).isEmpty();
  }

  @Test
  void customActivity() throws IOException, ProcessingException {
    Workflow workflow = WorkflowBuilder.fromYaml(getClass()
        .getResourceAsStream("/workflowbuilder/custom-activity.yaml"));

    assertThat(workflow.getFirstActivity()).hasValueSatisfying(c -> {
      assertThat(((DoSomething) (c.getActivity())).getMyParameter()).isEqualTo("abc");
    });
  }

  @Test
  void customActivity_notFound() {
    assertThatThrownBy(() -> WorkflowBuilder.fromYaml(
        getClass().getResourceAsStream("/workflowbuilder/custom-activity-not-found.yaml")))
        .describedAs("Should fail are validation time because the JSON schema is updated on the fly")
        .isInstanceOf(YamlNotValidException.class);
  }

  @Test
  void customActivity_duplicateDefinition() {
    assertThatThrownBy(() -> WorkflowBuilder.fromYaml(
        getClass().getResourceAsStream("/workflowbuilder/custom-activity-duplicate-definition.yaml")))
        .describedAs(
            "Workflow is invalid because 2 DuplicateCustomActivity classes are defined (in different packages")
        .isInstanceOf(JsonMappingException.class);
  }

}
