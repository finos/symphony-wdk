package com.symphony.bdk.workflow;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.lang.WorkflowBuilder;
import com.symphony.bdk.workflow.lang.swadl.Workflow;

import org.junit.jupiter.api.Test;

import java.util.Collections;

class FormReplyIntegrationTest extends IntegrationTest {

  @Test
  void sendFormSendMessageOnReply() throws Exception {
    Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/send-form-reply.yaml"));
    engine.execute(workflow);

    V4Message message = new V4Message();
    message.setMessageId("msgId");
    when(messageService.send(anyString(), anyString())).thenReturn(message);

    // trigger workflow execution
    engine.messageReceived("123", "/message");
    verify(messageService, timeout(5000)).send(eq("123"), contains("form"));

    // reply to form
    engine.formReceived("msgId", "sendForm", Collections.singletonMap("aField", "My message"));

    // bot should send my reply back
    verify(messageService, timeout(5000)).send(eq("123"), contains("My message"));
  }

  @Test
  void sendFormSendMessageOnReply_multipleUsers() throws Exception {
    Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/send-form-reply.yaml"));
    engine.execute(workflow);

    V4Message message = new V4Message();
    message.setMessageId("msgId");
    when(messageService.send(anyString(), anyString())).thenReturn(message);

    // trigger workflow execution
    engine.messageReceived("123", "/message");
    verify(messageService, timeout(5000)).send(eq("123"), contains("form"));

    // user 1 replies to form
    engine.formReceived("msgId", "sendForm", Collections.singletonMap("aField", "My message"));
    // user 2 replies to form
    engine.formReceived("msgId", "sendForm", Collections.singletonMap("aField", "My message"));

    // bot should send my reply back
    verify(messageService, timeout(5000).times(2)).send(eq("123"), contains("My message"));
  }

  @Test
  void sendFormSendMessageOnReply_followUpActivity() throws Exception {
    Workflow workflow = WorkflowBuilder.fromYaml(getClass()
        .getResourceAsStream("/send-form-reply-followup-activity.yaml"));
    engine.execute(workflow);

    V4Message message = new V4Message();
    message.setMessageId("msgId");
    when(messageService.send(anyString(), anyString())).thenReturn(message);

    // trigger workflow execution
    engine.messageReceived("123", "/message");
    verify(messageService, timeout(5000)).send(eq("123"), contains("form"));

    // user 1 replies to form
    engine.formReceived("msgId", "sendForm", Collections.singletonMap("aField", "My message"));

    // bot should send my reply back
    verify(messageService, timeout(5000)).send(eq("123"), contains("First reply: My message"));
    verify(messageService, timeout(5000)).send(eq("123"), contains("Second reply: My message"));
  }

  @Test
  void sendFormSendMessageOnReply_expiration() throws Exception {
    Workflow workflow = WorkflowBuilder.fromYaml(
        getClass().getResourceAsStream("/send-form-reply-expiration.yaml"));
    engine.execute(workflow);

    V4Message message = new V4Message();
    message.setMessageId("msgId");
    when(messageService.send(anyString(), anyString())).thenReturn(message);

    // trigger workflow execution
    engine.messageReceived("123", "/message");
    verify(messageService, timeout(5000)).send(eq("123"), contains("form"));

    // user never replies

    // bot should run the on/activity-expired activity after 1s
    verify(messageService, timeout(5000)).send(eq("123"), contains("Form expired"));
  }
}
