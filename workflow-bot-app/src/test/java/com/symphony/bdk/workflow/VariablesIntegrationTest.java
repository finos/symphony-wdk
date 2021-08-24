package com.symphony.bdk.workflow;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.swadl.WorkflowBuilder;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class VariablesIntegrationTest extends IntegrationTest {
  @Test
  @DisplayName(
      "Given as end-message activity when the streamId is a variable, then a message is sent to the stream")
  void shouldSendMessageToStreamWhenIdIsVariable() throws Exception {

    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/message/send-messages-with-variables.swadl.yaml"));
    final V4Message message = new V4Message().messageId("msgId");
    final String content = "<messageML>Have a nice day !</messageML>\n";

    when(messageService.send("1234", content)).thenReturn(message);
    engine.execute(workflow);
    engine.onEvent(messageReceived("/send"));

    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(messageService, timeout(5000)).send(argumentCaptor.capture(), any(Message.class));
    final String captorStreamId = argumentCaptor.getValue();

    assertThat(captorStreamId).isEqualTo("1234");
  }

  @Test
  void variablesAreTyped() throws Exception {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/typed-variables.swadl.yaml"));

    engine.execute(workflow);
    engine.onEvent(messageReceived("/typed"));
    String processId = lastProcess().get();

    await().atMost(5, SECONDS).until(() -> processIsCompleted(processId));
  }

}
