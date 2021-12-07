package com.symphony.bdk.workflow;

import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.assertThat;

import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class CustomProcessEnginePluginIntegrationTest extends IntegrationTest {

  @Test
  void textTest() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/plugin/utility-functions-text.swadl.yaml"));

    engine.deploy(workflow);

    engine.onEvent(messageReceived("/text"));
    assertThat(workflow).executed("getTextFromPresentationML");
  }

  @Test
  void mentionsTest() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/plugin/utility-functions-mentions.swadl.yaml"));

    engine.deploy(workflow);

    RealTimeEvent<V4MessageSent> event = messageReceived("abc", "/mentions @John");
    event.getSource().getMessage().data(userMentionData(123L));

    engine.onEvent(event);
    assertThat(workflow).executed("getMentionsFromEvent");
  }

  @Test
  void escapeTest() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/plugin/utility-functions-escape.swadl.yaml"));

    engine.deploy(workflow);

    engine.onEvent(messageReceived("/escape"));
    assertThat(workflow).executed("setVariable", "escapeText");

  }

}
