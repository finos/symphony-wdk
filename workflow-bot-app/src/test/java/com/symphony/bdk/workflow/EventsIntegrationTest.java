package com.symphony.bdk.workflow;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.gen.api.model.UserV2;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.swadl.WorkflowBuilder;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

class EventsIntegrationTest extends IntegrationTest {

  @Test
  void eventInTheMiddleOfWorkflow() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/event/event-middle-workflow.swadl.yaml"));
    when(messageService.send(anyString(), anyString())).thenReturn(message("msgId"));

    engine.execute(workflow);
    engine.onEvent(messageReceived("abc", "/one"));
    verify(messageService, timeout(5000)).send("abc", "One");

    verify(messageService, never()).send("abc", "Two");

    await().atMost(5, TimeUnit.SECONDS).ignoreExceptions().until(() -> {
      engine.onEvent(messageReceived("abc", "/two"));
      verify(messageService).send("abc", "Two");
      return true;
    });
  }

  @Test
  void twoWorkflowsSameEvent() throws IOException, ProcessingException {
    final Workflow workflow1 = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/event/msg1-event.swadl.yaml"));
    final Workflow workflow2 = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/event/msg2-event.swadl.yaml"));
    when(messageService.send(anyString(), anyString())).thenReturn(message("msgId"));

    engine.execute(workflow1);
    engine.execute(workflow2);
    engine.onEvent(messageReceived("abc", "/msg"));

    verify(messageService, timeout(5000)).send("abc", "msg1");
    verify(messageService, timeout(5000)).send("abc", "msg2");
  }

  @Test
  void multipleStartingEvents() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/event/multiple-events.swadl.yaml"));
    when(messageService.send(anyString(), anyString())).thenReturn(message("msgId"));

    engine.execute(workflow);

    engine.onEvent(messageReceived("abc", "/msg1"));
    engine.onEvent(messageReceived("abc", "/msg2"));

    verify(messageService, timeout(5000).times(2)).send("abc", "msg");
  }

  @Test
  void multipleEventsMiddleOfWorkflow() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/event/mutiple-events-middle-workflow.swadl.yaml"));
    when(messageService.send(anyString(), anyString())).thenReturn(message("msgId"));

    engine.execute(workflow);

    // first execution
    engine.onEvent(messageReceived("abc", "/one"));

    // second execution
    engine.onEvent(messageReceived("abc", "/one"));

    await().atMost(5, TimeUnit.SECONDS).ignoreExceptions().until(() -> {
      engine.onEvent(messageReceived("abc", "/msg1"));
      engine.onEvent(messageReceived("abc", "/msg2"));
      verify(messageService, times(2)).send("abc", "Two");
      return true;
    });
  }

  @Test
  void ifWithIntermediateEvent() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/event/if-intermediate-event.swadl.yaml"));
    when(messageService.send(anyString(), anyString())).thenReturn(message("msgId"));

    engine.execute(workflow);

    engine.onEvent(messageReceived("abc", "/execute"));

    await().atMost(5, TimeUnit.SECONDS).ignoreExceptions().until(() -> {
      engine.onEvent(messageReceived("abc", "/execute2"));
      verify(messageService).send("abc", "act2");
      return true;
    });
  }

  @Test
  void onMessageReceivedArguments() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/event/message-received-args.swadl.yaml"));
    when(messageService.send(anyString(), anyString())).thenReturn(message("msgId"));

    engine.execute(workflow);
    engine.onEvent(messageReceived("abc", "/go room name"));

    verify(messageService, timeout(5000).times(1)).send("abc", "Received room name");
  }

  @Test
  void onMessageReceivedArgumentsBotMention() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/event/message-received-args-bot-mention.swadl.yaml"));
    UserV2 bot = new UserV2();
    bot.setDisplayName("myBot");
    when(sessionService.getSession()).thenReturn(bot);
    when(messageService.send(anyString(), anyString())).thenReturn(message("msgId"));

    engine.execute(workflow);
    engine.onEvent(messageReceived("abc", "@myBot /go room name"));

    verify(messageService, timeout(5000).times(1)).send("abc", "Received room name");
  }

  @Test
  void onMessageReceivedArgumentsMention() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/event/message-received-args-mention.swadl.yaml"));
    when(messageService.send(anyString(), anyString())).thenReturn(message("msgId"));

    engine.execute(workflow);
    RealTimeEvent<V4MessageSent> event = messageReceived("abc", "/go @John");
    event.getSource().getMessage().data(userMentionData(123L));
    engine.onEvent(event);

    verify(messageService, timeout(5000).times(1)).send("abc", "Received John 123");
  }

  @Test
  void onMessageReceivedArgumentsMentions() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/event/message-received-args-mentions.swadl.yaml"));
    when(messageService.send(anyString(), anyString())).thenReturn(message("msgId"));

    engine.execute(workflow);
    engine.onEvent(messageReceived("abc", "/go @John @Bob Lee @Eve"));

    verify(messageService, timeout(5000).times(1)).send("abc", "Received John, Bob Lee, Eve");
  }

  @Test
  void onMessageReceivedArgumentsMixed() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/event/message-received-args-mixed.swadl.yaml"));
    when(messageService.send(anyString(), anyString())).thenReturn(message("msgId"));

    engine.execute(workflow);
    engine.onEvent(messageReceived("abc", "/go Hello @John #awesome $TESLA"));

    verify(messageService, timeout(5000).times(1)).send("abc", "Received Hello, John, awesome, TESLA");
  }

  @Test
  void onMessageReceivedHashTagsFunction() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/event/message-received-hashtags.swadl.yaml"));

    engine.execute(workflow);
    RealTimeEvent<V4MessageSent> event = messageReceived("abc", "/go #awesome #super");
    event.getSource().getMessage().data("{\n"
        + "  \"0\": {\n"
        + "    \"id\": [\n"
        + "      {\n"
        + "        \"type\": \"org.symphonyoss.taxonomy.hashtag\",\n"
        + "        \"value\": \"awesome\"\n"
        + "      }\n"
        + "    ],\n"
        + "    \"type\": \"org.symphonyoss.taxonomy\",\n"
        + "    \"version\": \"1.0\"\n"
        + "  },\n"
        + "  \"1\": {\n"
        + "    \"id\": [\n"
        + "      {\n"
        + "        \"type\": \"org.symphonyoss.taxonomy.hashtag\",\n"
        + "        \"value\": \"super\"\n"
        + "      }\n"
        + "    ],\n"
        + "    \"type\": \"org.symphonyoss.taxonomy\",\n"
        + "    \"version\": \"1.0\"\n"
        + "  }\n"
        + "}\n");
    engine.onEvent(event);

    verify(messageService, timeout(5000).times(1)).send("abc", "Received awesome super");
  }

  @Test
  void onMessageReceivedCashTagsFunction() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/event/message-received-cashtags.swadl.yaml"));

    engine.execute(workflow);
    RealTimeEvent<V4MessageSent> event = messageReceived("abc", "/go $GOOG $TSLA");
    event.getSource().getMessage().data("{\n"
        + "  \"0\": {\n"
        + "    \"id\": [\n"
        + "      {\n"
        + "        \"type\": \"org.symphonyoss.fin.security.id.ticker\",\n"
        + "        \"value\": \"GOOG\"\n"
        + "      }\n"
        + "    ],\n"
        + "    \"type\": \"org.symphonyoss.fin.security\",\n"
        + "    \"version\": \"1.0\"\n"
        + "  },\n"
        + "  \"1\": {\n"
        + "    \"id\": [\n"
        + "      {\n"
        + "        \"type\": \"org.symphonyoss.fin.security.id.ticker\",\n"
        + "        \"value\": \"TSLA\"\n"
        + "      }\n"
        + "    ],\n"
        + "    \"type\": \"org.symphonyoss.fin.security\",\n"
        + "    \"version\": \"1.0\"\n"
        + "  }\n"
        + "}\n");
    engine.onEvent(event);

    verify(messageService, timeout(5000).times(1)).send("abc", "Received GOOG TSLA");
  }

  private String userMentionData(long userId) {
    return "{\n"
        + "  \"0\": {\n"
        + "    \"id\": [\n"
        + "      {\n"
        + "        \"type\": \"com.symphony.user.userId\",\n"
        + "        \"value\": \"" + userId + "\"\n"
        + "      }\n"
        + "    ],\n"
        + "    \"type\": \"com.symphony.user.mention\"\n"
        + "  }\n"
        + "}\n";
  }

}
