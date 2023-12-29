package com.symphony.bdk.workflow;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.engine.shared.SharedDataRepository;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SharedDataIntegrationTest extends IntegrationTest {

  @Autowired SharedDataRepository sharedDataRepository;

  @Test
  @DisplayName("Share counter between process instances")
  void shareCounterBetweenProcesses() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/shareddata/shared-counter-data.swadl.yaml"));
    final V4Message message = message("/count");
    when(messageService.send(anyString(), any(Message.class))).thenReturn(message);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/count"));

    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(messageService, timeout(5000).times(2)).send(anyString(), captor.capture());
    assertThat(captor.getValue().getContent()).contains("1");

    engine.onEvent(messageReceived("/count"));
    verify(messageService, timeout(5000).times(4)).send(anyString(), captor.capture());
    assertThat(captor.getValue().getContent()).contains("2");
    sharedDataRepository.deleteAll();
  }

  @Test
  @DisplayName("Share counter between workflows")
  void shareCounterBetweenWorkflows() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/shareddata/shared-counter-data.swadl.yaml"));
    final Workflow workflow2 =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/shareddata/shared-counter-data2.swadl.yaml"));
    final V4Message message = message("/count");
    when(messageService.send(anyString(), any(Message.class))).thenReturn(message);

    engine.deploy(workflow);
    engine.deploy(workflow2);

    engine.onEvent(messageReceived("/count"));
    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(messageService, timeout(5000).times(2)).send(anyString(), captor.capture());
    assertThat(captor.getValue().getContent()).contains("1");

    engine.onEvent(messageReceived("/count2"));
    verify(messageService, timeout(5000).times(4)).send(anyString(), captor.capture());
    assertThat(captor.getValue().getContent()).contains("2");
    sharedDataRepository.deleteAll();
  }

}
