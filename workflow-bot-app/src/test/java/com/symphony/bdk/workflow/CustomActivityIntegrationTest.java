package com.symphony.bdk.workflow;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.junit.jupiter.api.Test;

class CustomActivityIntegrationTest extends IntegrationTest {

  @Test
  void customActivity() throws Exception {
    Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream("/custom-activity.swadl.yaml"));
    engine.deploy(workflow, "defaultId");

    engine.onEvent(messageReceived("/execute"));

    // com.symphony.bdk.workflow.DoSomethingExecutor is just sending a message
    verify(messageService, timeout(5000)).send("123", "abc");
  }

}
