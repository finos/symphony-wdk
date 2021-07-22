package com.symphony.bdk.workflow;

import static com.symphony.bdk.workflow.IntegrationTest.message;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.workflow.lang.WorkflowBuilder;
import com.symphony.bdk.workflow.lang.swadl.Workflow;

import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

class VariablesIntegrationTest extends IntegrationTest {
  @Test
  @DisplayName(
      "Given as end-message activity when the streamId is a variable, then a message is sent to the stream")
  void shouldSendMessageToStreamWhenIdIsVariable() throws Exception {

    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/send-messages-with-variables.yaml"));
    final V4Message message = new V4Message().messageId("msgId");
    final String content = "<messageML>Have a nice day !</messageML>\n";

    when(messageService.send("1234", content)).thenReturn(message);
    engine.execute(workflow);
    engine.onEvent(message("/send"));

    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(messageService, timeout(5000)).send(argumentCaptor.capture(), anyString());
    final String captorStreamId = argumentCaptor.getValue();

    assertThat(captorStreamId).isEqualTo("1234");
  }

  @Test
  void variablesAreTyped() throws Exception {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/typed-variables.yaml"));

    engine.execute(workflow);
    String processId = engine.onEvent(message("/send")).get();

    await().atMost(5, SECONDS).until(() -> processIsCompleted(processId));
  }

  private Boolean processIsCompleted(String processId) {
    List<HistoricProcessInstance> processes = historyService.createHistoricProcessInstanceQuery()
        .processInstanceId(processId).list();
    if (!processes.isEmpty()) {
      HistoricProcessInstance processInstance = processes.get(0);
      return processInstance.getState().equals("COMPLETED");
    }
    return false;
  }
}
