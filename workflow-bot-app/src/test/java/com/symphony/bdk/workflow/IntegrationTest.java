package com.symphony.bdk.workflow;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.gen.api.model.V4Initiator;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.gen.api.model.V4SymphonyElementsAction;
import com.symphony.bdk.gen.api.model.V4User;
import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.engine.camunda.CamundaEngine;

import org.camunda.bpm.engine.HistoryService;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Map;

@SpringBootTest
abstract class IntegrationTest {

  @Autowired
  CamundaEngine engine;

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
  public static RealTimeEvent<V4MessageSent> message(String content) {
    V4Initiator initiator = new V4Initiator();
    initiator.setUser(new V4User());

    V4MessageSent messageSent = new V4MessageSent();
    V4Message message = new V4Message();
    message.setMessage(content);
    messageSent.setMessage(message);
    return new RealTimeEvent<>(initiator, messageSent);
  }

  public static RealTimeEvent<V4SymphonyElementsAction> form(String messageId,
      String formId, Map<String, Object> formReplies) {
    V4Initiator initiator = new V4Initiator();
    initiator.setUser(new V4User());

    V4SymphonyElementsAction elementsAction = new V4SymphonyElementsAction();
    elementsAction.setFormMessageId(messageId);
    elementsAction.setFormId(formId);
    elementsAction.setFormValues(formReplies);
    return new RealTimeEvent<>(initiator, elementsAction);
  }

}
