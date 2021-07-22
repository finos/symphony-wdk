package com.symphony.bdk.workflow;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.workflow.engine.WorkflowEngine;

import org.camunda.bpm.engine.HistoryService;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
abstract class IntegrationTest {

  @Autowired
  WorkflowEngine engine;

  @Autowired
  HistoryService historyService;

  // Mock the BDK
  @MockBean
  AuthSession botSession;

  @MockBean(name = "streamService")
  StreamService streamService;

  @MockBean(name = "messageService")
  MessageService messageService;

  // make sure we start the test with a clean engine to avoid the same /command to be registered
  @AfterEach
  void removeAllWorkflows() throws InterruptedException {
    for (int i = 0; i < 5; i++) {
      try {
        engine.stopAll();
        return;
      } catch (Exception e) {
        // this might fail if processes are running at the same time, wait a bit a retry one more time
        Thread.sleep(100); // NOSONAR
      }
    }
  }

}
