package com.symphony.bdk.workflow;

import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.content;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.junit.jupiter.api.Test;

class ExecuteScriptIntegrationTest extends IntegrationTest {

  @Test
  void executeScript() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/execute-script.swadl.yaml"));

    engine.execute(workflow);
    engine.onEvent(messageReceived("/execute"));

    verify(messageService, timeout(5000)).send("123", "bar");
  }

  @Test
  void executeScript_setsVariable() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/execute-script-sets-variable.swadl.yaml"));

    engine.execute(workflow);
    engine.onEvent(messageReceived("/execute"));

    verify(messageService, timeout(5000)).send("123", "bar");
    verify(messageService, timeout(5000)).send(eq("abc"), content("bar"));
  }
}
