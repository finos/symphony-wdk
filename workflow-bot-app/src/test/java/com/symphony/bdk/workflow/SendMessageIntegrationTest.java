package com.symphony.bdk.workflow;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.gen.api.model.Stream;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.swadl.WorkflowBuilder;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

class SendMessageIntegrationTest extends IntegrationTest {

  @Test
  @DisplayName(
      "Given a send-message with a streamId, when the triggering message is received, "
          + "then the provided message should be sent to the room")
  void sendMessageOnMessage() throws Exception {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/message/send-message-on-message.swadl.yaml"));
    final V4Message message = message("msgId");

    final String streamId = "123";
    final String content = "<messageML>Hello!</messageML>";
    when(messageService.send(streamId, content)).thenReturn(message);

    engine.execute(workflow);
    engine.onEvent(messageReceived("/message"));

    verify(messageService, timeout(5000)).send(streamId, content);
  }

  @Test
  @DisplayName(
      "Given two activities: create-room and send-message, when the room is created, then a message is sent to it")
  void sendMessageToCreatedRoomOnMessage() throws Exception {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/room/create-room-and-send-message.swadl.yaml"));
    final V4Message message = new V4Message().messageId("msgId");
    final List<Long> uids = Arrays.asList(1234L, 5678L);
    final Stream stream = new Stream().id("0000");
    final String content = "<messageML><p>Hello!</p></messageML>";

    when(streamService.create(uids)).thenReturn(stream);
    when(messageService.send("0000", content)).thenReturn(message);

    engine.execute(workflow);
    engine.onEvent(messageReceived("/create-room"));

    verify(streamService, timeout(5000).times(1)).create(uids);
    verify(messageService, timeout(5000).times(1)).send(anyString(), eq(content));
  }
}
