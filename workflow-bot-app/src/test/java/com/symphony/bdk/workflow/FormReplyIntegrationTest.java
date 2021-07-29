package com.symphony.bdk.workflow;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.swadl.WorkflowBuilder;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.junit.jupiter.api.Test;

import java.util.Collections;

class FormReplyIntegrationTest extends IntegrationTest {

  @Test
  void sendFormSendMessageOnReply() throws Exception {
    Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/form/send-form-reply.yaml"));
    engine.execute(workflow);

    V4Message message = message("msgId");
    when(messageService.send(anyString(), anyString())).thenReturn(message);

    // trigger workflow execution
    engine.onEvent(messageReceived("/message"));
    verify(messageService, timeout(5000)).send(eq("123"), contains("form"));

    // reply to form
    engine.onEvent(form("msgId", "sendForm", Collections.singletonMap("aField", "My message")));

    // bot should send my reply back
    verify(messageService, timeout(5000)).send(eq("123"), contains("My message"));
  }

  @Test
  void sendFormSendMessageOnReply_multipleUsers() throws Exception {
    Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/form/send-form-reply.yaml"));
    engine.execute(workflow);

    V4Message message = message("msgId");
    when(messageService.send(anyString(), anyString())).thenReturn(message);

    // trigger workflow execution
    engine.onEvent(messageReceived("/message"));
    verify(messageService, timeout(5000)).send(eq("123"), contains("form"));

    // user 1 replies to form
    engine.onEvent(form("msgId", "sendForm", Collections.singletonMap("aField", "My message")));

    // user 2 replies to form
    engine.onEvent(form("msgId", "sendForm", Collections.singletonMap("aField", "My message")));

    // bot should send my reply back
    verify(messageService, timeout(5000).times(2)).send(eq("123"), contains("My message"));
  }

  @Test
  void sendFormSendMessageOnReply_followUpActivity() throws Exception {
    Workflow workflow = WorkflowBuilder.fromYaml(getClass()
        .getResourceAsStream("/form/send-form-reply-followup-activity.yaml"));
    engine.execute(workflow);

    V4Message message = message("msgId");
    when(messageService.send(anyString(), anyString())).thenReturn(message);

    // trigger workflow execution
    engine.onEvent(messageReceived("/message"));
    verify(messageService, timeout(5000)).send(eq("123"), contains("form"));

    // user 1 replies to form
    engine.onEvent(form("msgId", "sendForm", Collections.singletonMap("aField", "My message")));

    // bot should send my reply back
    verify(messageService, timeout(5000)).send(eq("123"), contains("First reply: My message"));
    verify(messageService, timeout(5000)).send(eq("123"), contains("Second reply: My message"));
  }

  @Test
  void sendFormSendMessageOnReply_expiration() throws Exception {
    Workflow workflow = WorkflowBuilder.fromYaml(
        getClass().getResourceAsStream("/form/send-form-reply-expiration.yaml"));
    engine.execute(workflow);

    V4Message message = message("msgId");
    when(messageService.send(anyString(), anyString())).thenReturn(message);

    // trigger workflow execution
    engine.onEvent(messageReceived("/message"));
    verify(messageService, timeout(5000)).send(eq("123"), contains("form"));

    // user never replies

    // bot should run the on/activity-expired activity after 1s
    verify(messageService, timeout(5000)).send(eq("123"), contains("Form expired"));
  }

  @Test
  void sendFormNested() throws Exception {
    Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/form/send-form-reply-nested.swadl.yaml"));
    engine.execute(workflow);

    V4Message message = message("msgId");
    when(messageService.send(anyString(), anyString())).thenReturn(message);

    // trigger workflow execution
    engine.onEvent(messageReceived("/message"));
    verify(messageService, timeout(5000)).send(eq("abc"), contains("form"));

    // reply to first form
    engine.onEvent(form("msgId", "sendForm", Collections.singletonMap("question", "My message")));

    // bot should not run the on/activity-expired activity after 1s because it is attached to the second form that
    // has not been replied to
    verify(messageService, timeout(5000)).send(eq("abc"), contains("expiration-outer"));
  }
}
