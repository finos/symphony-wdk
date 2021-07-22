package com.symphony.bdk.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.lang.WorkflowBuilder;
import com.symphony.bdk.workflow.lang.swadl.Workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class VariablesIntegrationTest extends IntegrationTest {
  @Test
  @DisplayName(
      "Given as end-message activity when the streamId is a variable, then a message is sent to the stream")
  void shouldSendMessageToStreamWhenIdIsVariable() throws Exception {

    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/send-messages-with-variables.yaml"));
    final V4Message message = new V4Message().messageId("msgId");
    final String content = "<messageML>Have a nice day !</messageML>\n";

    when(messageService.send("1234", content)).thenReturn(message);
    engine.execute(workflow);
    engine.messageReceived("9999", "/send");

    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(messageService, timeout(5000)).send(argumentCaptor.capture(), anyString());
    final String captorStreamId = argumentCaptor.getValue();

    assertThat(captorStreamId).isEqualTo("1234");
  }
}
