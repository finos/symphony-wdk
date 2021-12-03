package com.symphony.bdk.workflow;

import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

class LoopIntegrationTest extends IntegrationTest {

  static Stream<Arguments> executedActivities() {
    return Stream.of(
        arguments("/loop/loop.swadl.yaml", List.of("act1", "act2", "act1", "act2")),
        arguments("/loop/loop-continue-activity.swadl.yaml", List.of("act1", "act2", "act1", "act2", "act3")),
        arguments("/loop/loop-more-activities.swadl.yaml", List.of("act1", "act2", "act3", "act1", "act2", "act3"))
    );
  }

  @ParameterizedTest
  @MethodSource("executedActivities")
  void looping(String workflowFile, List<String> activities) throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(workflowFile));

    when(messageService.send(anyString(), any(Message.class))).thenReturn(message("msgId"));

    engine.deploy(workflow);

    engine.onEvent(messageReceived("/execute"));

    assertThat(workflow).isExecutedWithProcessAndActivities(lastProcess(), activities);
  }

  @Test
  void loopOverridesOutputsTest() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/loop/loop-overrides-outputs.swadl.yaml"));

    Message message0 = Message.builder().content("<messageML>0</messageML>").build();
    Message message1 = Message.builder().content("<messageML>1</messageML>").build();

    when(messageService.send(anyString(), refEq(message0, "data", "attachments", "previews", "version"))).thenReturn(
        message("msgIdO"));
    when(messageService.send(anyString(), refEq(message1, "data", "attachments", "previews", "version"))).thenReturn(
        message("msgId1"));

    engine.deploy(workflow);

    engine.onEvent(messageReceived("/execute"));
    V4Message expectedOutputs = new V4Message();
    expectedOutputs.messageId("msgId1");

    assertThat(workflow).executed("act0", "act1", "act2", "act0", "act1", "act2")
        .hasOutput("act1.outputs.msgId", "msgId1")
        .hasOutput("act1.outputs.message", expectedOutputs);
  }

}
