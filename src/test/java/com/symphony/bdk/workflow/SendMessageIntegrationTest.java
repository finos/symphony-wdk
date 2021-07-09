package com.symphony.bdk.workflow;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.lang.WorkflowBuilder;
import com.symphony.bdk.workflow.lang.swadl.Workflow;

import org.junit.jupiter.api.Test;

class SendMessageIntegrationTest extends IntegrationTest {

  @Test
  void sendMessageOnMessage() throws Exception {
    Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/send-message-on-message.yaml"));
    engine.execute(workflow);

    V4Message message = new V4Message();
    message.setMessageId("msgId");
    when(messageService.send(anyString(), anyString())).thenReturn(message);

    engine.messageReceived("123", "/message");

    verify(messageService, timeout(5000)).send(anyString(), anyString());
  }
}
