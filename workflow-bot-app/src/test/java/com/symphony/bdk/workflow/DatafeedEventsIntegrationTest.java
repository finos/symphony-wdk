package com.symphony.bdk.workflow;

import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.content;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.UserV2;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

class DatafeedEventsIntegrationTest extends IntegrationTest {

  @Test
  void eventInTheMiddleOfWorkflow() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/event-middle-workflow.swadl.yaml"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("abc", "/one"));
    verify(messageService, timeout(5000)).send(eq("abc"), content("One"));

    verify(messageService, never()).send(eq("abc"), content("Two"));

    await().atMost(5, TimeUnit.SECONDS).ignoreExceptions().until(() -> {
      engine.onEvent(messageReceived("abc", "/two"));
      verify(messageService).send(eq("abc"), content("Two"));
      return true;
    });
  }

  @Test
  void eventInTheMiddleUsingOutputs() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/event-middle-using-outputs.swadl.yaml"));

    final V4Message actualMessage = createMessage("MSG_ID");
    when(messageService.send(anyString(), any(Message.class))).thenReturn(actualMessage);
    when(messageService.getMessage(anyString())).thenReturn(actualMessage);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("abc", "/one"));
    verify(messageService, timeout(5000)).send(eq("abc"), content("One"));

    await().atMost(5, TimeUnit.SECONDS).ignoreExceptions().until(() -> {
      engine.onEvent(messageReceived("abc", "/two"));
      verify(messageService).getMessage("MSG_ID");
      return true;
    });
  }

  @Test
  void twoWorkflowsSameEvent() throws IOException, ProcessingException {
    final Workflow workflow1 = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/msg1-event.swadl.yaml"));
    final Workflow workflow2 = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/msg2-event.swadl.yaml"));

    engine.deploy(workflow1);
    engine.deploy(workflow2);
    engine.onEvent(messageReceived("abc", "/twoWorkflowsSameEvent"));

    verify(messageService, timeout(5000)).send(eq("abc"), content("msg1"));
    verify(messageService, timeout(5000)).send(eq("abc"), content("msg2"));
  }

  @Test
  void multipleStartingEvents() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/multiple-events.swadl.yaml"));

    engine.deploy(workflow);

    engine.onEvent(messageReceived("abc", "/msg1"));
    engine.onEvent(messageReceived("abc", "/msg2"));

    verify(messageService, timeout(5000).times(2)).send(eq("abc"), content("msg"));
  }

  @Test
  void multipleEventsMiddleOfWorkflow() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/mutiple-events-middle-workflow.swadl.yaml"));

    engine.deploy(workflow);

    // first execution
    engine.onEvent(messageReceived("abc", "/one"));

    // second execution
    engine.onEvent(messageReceived("abc", "/one"));

    await().atMost(5, TimeUnit.SECONDS).ignoreExceptions().until(() -> {
      engine.onEvent(messageReceived("abc", "/msg1"));
      engine.onEvent(messageReceived("abc", "/msg2"));
      verify(messageService, times(2)).send(eq("abc"), content("Two"));
      return true;
    });
  }

  @Test
  void ifWithIntermediateEvent() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/if-intermediate-event.swadl.yaml"));

    engine.deploy(workflow);

    engine.onEvent(messageReceived("abc", "/execute"));

    await().atMost(5, TimeUnit.SECONDS).ignoreExceptions().until(() -> {
      engine.onEvent(messageReceived("abc", "/execute2"));
      verify(messageService).send(eq("abc"), content("act2"));
      return true;
    });
  }

  @Test
  void onMessageReceivedArguments() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/message-received-args.swadl.yaml"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("abc", "/go room name"));

    verify(messageService, timeout(5000).times(1)).send(eq("abc"), content("Received room name"));
  }

  @Test
  void onMessageReceivedArgumentsBotMention() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/message-received-args-bot-mention.swadl.yaml"));
    UserV2 bot = new UserV2();
    bot.setDisplayName("myBot");
    when(sessionService.getSession()).thenReturn(bot);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("abc", "@myBot /go room name"));

    verify(messageService, timeout(5000).times(1)).send(eq("abc"), content("Received room name"));
  }

  @Test
  void onMessageReceivedArgumentsMention() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/message-received-args-mention.swadl.yaml"));

    engine.deploy(workflow);
    RealTimeEvent<V4MessageSent> event = messageReceived("abc", "/go @John");
    event.getSource().getMessage().data(userMentionData(123L));
    engine.onEvent(event);

    verify(messageService, timeout(5000).times(1)).send(eq("abc"), content("Received John 123"));
  }

  @Test
  void onMessageReceivedArgumentsMentions() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/message-received-args-mentions.swadl.yaml"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("abc", "/go @John @Bob Lee @Eve"));

    verify(messageService, timeout(5000).times(1)).send(eq("abc"), content("Received John, Bob Lee, Eve"));
  }

  @Test
  void onMessageReceivedArgumentsMixed() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/message-received-args-mixed.swadl.yaml"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("abc", "/go Hello @John #awesome $TESLA"));

    verify(messageService, timeout(5000).times(1)).send(eq("abc"), content("Received Hello, John, awesome, TESLA"));
  }

  @Test
  void onMessageReceivedHashTagsFunction() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/message-received-hashtags.swadl.yaml"));

    engine.deploy(workflow);
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

    verify(messageService, timeout(5000).times(1)).send(eq("abc"), content("Received awesome super"));
  }

  @Test
  void onMessageReceivedCashTagsFunction() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/message-received-cashtags.swadl.yaml"));

    engine.deploy(workflow);
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

    verify(messageService, timeout(5000).times(1)).send(eq("abc"), content("Received GOOG TSLA"));
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
