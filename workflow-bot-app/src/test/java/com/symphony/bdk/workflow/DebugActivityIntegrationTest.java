package com.symphony.bdk.workflow;

import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.junit.jupiter.api.Test;

class DebugActivityIntegrationTest extends IntegrationTest {

  @Test
  void debugGlobalVariables() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/debug/debug-global-variables.swadl.yaml"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/debug-global-variables"));

    assertThat(workflow).isExecuted()
        .hasOutput(String.format("%s.outputs.object", "debugGlobalVariables1"), "Hello World")
        .hasOutput(String.format("%s.outputs.object", "debugGlobalVariables2"), "value");
  }

  @Test
  void debugActivityOutputs() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/debug/debug-activity-outputs.swadl.yaml"));

    final V4Message message = message("msgId", "Hello World!");
    when(messageService.send(anyString(), any(Message.class))).thenReturn(message);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/debug-activity-outputs"));

    assertThat(workflow).isExecuted();
  }
}
