package com.symphony.bdk.workflow;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.workflow.engine.WorkflowEngine;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
abstract class IntegrationTest {

  @Autowired
  WorkflowEngine engine;

  // Mock the BDK
  @MockBean
  AuthSession botSession;
  @MockBean
  StreamService streamService;
  @MockBean
  MessageService messageService;

  @AfterEach
  void tearDown() {
    engine.stopAll(); // make sure we start the test with a clean engine to avoid the same /command to be registered
  }

}
