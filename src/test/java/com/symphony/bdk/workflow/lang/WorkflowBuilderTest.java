package com.symphony.bdk.workflow.lang;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.workflow.lang.swadl.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;


@SpringBootTest
public class WorkflowBuilderTest {

  @MockBean
  AuthSession botSession;

  @MockBean
  MessageService messageService;

  @MockBean
  StreamService streamService;

  @Test
  void shouldLoadGlobalVariablesWhenLoadingValidSwadl()
      throws IOException, ProcessingException {
    Workflow workflow = WorkflowBuilder.fromYaml(getClass()
        .getResourceAsStream("/workflowbuilder/valid_swadl.yaml"));

    assertThat(workflow.getVariables()).isNotNull();
    assertThat(workflow.getVariables()).isNotEmpty();
    assertThat(workflow.getVariables().size()).isEqualTo(2);
    assertThat(workflow.getVariables().get(0).get("var1")).isEqualTo("variable1");
    assertThat(workflow.getVariables().get(1).get("var2")).isEqualTo("variable2");
  }

  @Test
  void shouldIgnoreVariablesWhenLoadingInvalidSwadl()
      throws IOException, ProcessingException {
    Workflow workflow = WorkflowBuilder.fromYaml(getClass()
        .getResourceAsStream("/workflowbuilder/invalid_swadl.yaml"));

    assertThat(workflow.getVariables()).isNull();
  }
}
