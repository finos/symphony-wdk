package com.symphony.bdk.workflow;

import static com.symphony.bdk.workflow.custom.assertion.Assertions.assertThat;
import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.content;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class DeploymentIntegrationTest extends IntegrationTest {

  @Test
  void deployTwoWorkflowsSameId() throws IOException, ProcessingException {
    final Workflow workflowOne = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/deployment/deployment-different-workflow-same-id-1.swadl.yaml"));
    final Workflow workflowTwo = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/deployment/deployment-different-workflow-same-id-2.swadl.yaml"));

    when(messageService.send(anyString(), any(Message.class))).thenReturn(message("ignored message"));

    engine.deploy(workflowOne, "workflowId");
    engine.deploy(workflowTwo, "workflowId");

    engine.onEvent(messageReceived("/test"));

    verify(messageService, timeout(5000)).send(anyString(), content("message2"));
    verify(messageService, never()).send(anyString(), content("message1"));
    assertThat(workflowTwo).isExecuted();
  }
}
