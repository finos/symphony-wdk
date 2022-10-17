package com.symphony.bdk.workflow;

import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.assertThat;
import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.contains;
import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.content;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

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
    Thread.sleep(1000);
    verify(messageService, never()).send(eq(streamId), content("end join"));
    engine.onEvent(userJoined());
    Thread.sleep(1000);
    verify(messageService).send(eq(streamId), content("end join"));
    assertThat(workflow).executed("start", "scriptTrue", "scriptTrue_fork_gateway", "endMessage_join_gateway",
        "endMessage_join_gateway", "endMessage");
  }

  @SneakyThrows
  @Test
  @DisplayName("Form reply join operations")
  void test_parallel_join_with_form_reply_flow() {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/graph/form-reply-all-of.swadl.yaml"));
    when(messageService.send(anyString(), any(Message.class))).thenReturn(message("msgId"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/start"));
    Thread.sleep(1000);
    engine.onEvent(messageReceived("/done"));
    Thread.sleep(1000);
    verify(messageService, never()).send(anyString(), content("end join"));

    await().atMost(3, TimeUnit.SECONDS).ignoreExceptions().until(() -> {
      engine.onEvent(form("msgId", "sendForm", Collections.singletonMap("action", "approve")));
      // bot should send my form back because reject was selected
      verify(messageService).send(anyString(), content("end join"));
      return true;
    });
    assertThat(workflow).executed("sendForm", "sendForm_fork_gateway", "endMessage_join_gateway",
        "endMessage_join_gateway", "endMessage");
  }
}
