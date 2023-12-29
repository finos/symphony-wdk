package com.symphony.bdk.workflow;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.engine.executor.SecretKeeper;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SecretIntegrationTest extends IntegrationTest {
  @Autowired SecretKeeper secretKeeper;

  @Test
  @DisplayName("Show secret workflow")
  void shareCounterBetweenProcesses() throws Exception {
    secretKeeper.save("key", "secret".getBytes(StandardCharsets.UTF_8));
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/secret/secret.swadl.yaml"));
    final V4Message message = message("/show");
    when(messageService.send(anyString(), any(Message.class))).thenReturn(message);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/show"));

    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(messageService, timeout(5000)).send(anyString(), captor.capture());
    assertThat(captor.getValue().getContent()).contains("secret");
    secretKeeper.remove("key");
  }
}
