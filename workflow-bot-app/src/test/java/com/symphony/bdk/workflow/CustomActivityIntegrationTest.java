package com.symphony.bdk.workflow;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.symphony.bdk.workflow.lang.WorkflowBuilder;
import com.symphony.bdk.workflow.lang.swadl.Workflow;

import org.junit.jupiter.api.Test;

class CustomActivityIntegrationTest extends IntegrationTest {

  @Test
  void customActivity() throws Exception {
    Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/custom-activity.yaml"));
    engine.execute(workflow);

    engine.onEvent(message("/execute"));

    // com.symphony.bdk.workflow.DoSomethingExecutor is just sending a message
    verify(messageService, timeout(5000)).send("123", "abc");
  }

}
