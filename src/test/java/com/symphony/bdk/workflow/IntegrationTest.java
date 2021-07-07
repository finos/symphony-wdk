package com.symphony.bdk.workflow;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.lang.WorkflowBuilder;
import com.symphony.bdk.workflow.lang.swadl.Workflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class IntegrationTest {

  @Autowired
  WorkflowEngine engine;

  // Mock the BDK
  @MockBean
  AuthSession botSession;
  @MockBean
  StreamService streamService;
  @MockBean
  MessageService messageService;

  @Test
  void sendMessageOnMessage() throws Exception {
    Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/send-message-on-message.yaml"));
    engine.execute(workflow);

    engine.messageReceived("123", "/message");

    verify(messageService, timeout(5000)).send(anyString(), anyString());
  }
}
