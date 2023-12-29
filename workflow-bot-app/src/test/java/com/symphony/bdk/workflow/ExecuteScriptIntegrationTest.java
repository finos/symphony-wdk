package com.symphony.bdk.workflow;

import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.assertThat;
import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.content;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class ExecuteScriptIntegrationTest extends IntegrationTest {

  @Test
  void executeScript() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/execute-script.swadl.yaml"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/execute"));

    verify(messageService, timeout(5000)).send("123", "bar");
  }

  @Test
  void executeScript_setsVariable() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/execute-script-sets-variable.swadl.yaml"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/execute"));

    verify(messageService, timeout(5000)).send("123", "bar");
    verify(messageService, timeout(5000)).send(eq("abc"), content("bar"));
  }

  @Test
  void textTest() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/util/utility-functions-text.swadl.yaml"));

    engine.deploy(workflow);

    engine.onEvent(messageReceived("/text"));
    assertThat(workflow).executed("getTextFromPresentationML");
  }

  @Test
  void mentionsTest() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/util/utility-functions-mentions.swadl.yaml"));

    engine.deploy(workflow);

    RealTimeEvent<V4MessageSent> event = messageReceived("abc", "/mentions @John");
    event.getSource().getMessage().data(userMentionData(123L));

    engine.onEvent(event);
    assertThat(workflow).executed("getMentionsFromEvent");
  }

  @Test
  void escapeTest() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/util/utility-functions-escape.swadl.yaml"));

    engine.deploy(workflow);

    engine.onEvent(messageReceived("/escape"));
    assertThat(workflow).executed("setVariable", "escapeText");

  }
}
