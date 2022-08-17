package com.symphony.bdk.workflow;

import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.assertThat;
import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.content;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class JoinIntegrationTest extends IntegrationTest {

  @SneakyThrows
  @Test
  @DisplayName("Basic join operations")
  void test_parallel_join_flow() {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/graph/all-of.swadl.yaml"));
    final V4Message message = message("msgId");
    final String streamId = "123";
    when(messageService.send(eq(streamId), any(Message.class))).thenReturn(message);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/start"));
    Thread.sleep(1000);
    engine.onEvent(messageReceived("/update"));
    Thread.sleep(1000);
    engine.onEvent(messageReceived("/message"));
    verify(messageService, never()).send(eq(streamId), content("end join"));
    engine.onEvent(userJoined());
    Thread.sleep(1000);
    verify(messageService).send(eq(streamId), content("end join"));
    assertThat(workflow).executed("start", "script", "script_fork_gateway",  "endMessage_join_gateway",
        "endMessage_join_gateway", "endMessage");
  }
}
