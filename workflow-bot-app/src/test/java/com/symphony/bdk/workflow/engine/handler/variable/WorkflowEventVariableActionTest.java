package com.symphony.bdk.workflow.engine.handler.variable;

import com.symphony.bdk.workflow.engine.WorkflowDirectedGraph;
import com.symphony.bdk.workflow.engine.WorkflowNode;
import com.symphony.bdk.workflow.engine.camunda.WorkflowDirectedGraphService;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.engine.executor.EventHolder;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.impl.history.event.HistoricVariableUpdateEventEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WorkflowEventVariableActionTest {

  @Mock
  WorkflowDirectedGraphService cachingService;

  @Mock
  RuntimeService runtimeService;

  @InjectMocks WorkflowEventVariableAction eventVariableAction;

  private static final String EVENT_HOLDER_STRING = "{\"initiator\":{\"user\":{\"userId\":123,"
      + "\"firstName\":\"firstName\",\"lastName\":\"lastName\",\"displayName\":\"displayName\","
      + "\"email\":\"email@email.com\",\"username\":\"username\"}},"
      + "\"source\":{\"@eventImpl\":\"com.symphony.bdk.gen.api.model.V4MessageSent\","
      + "\"message\":{\"messageId\":\"MSG_ID\",\"parentMessageId\":null,\"timestamp\":1672825563000,"
      + "\"message\":\"<div data-format=\\\"PresentationML\\\" data-version=\\\"2.0\\\" class=\\\"wysiwyg\\\">"
      + "<p>/form</p></div>\",\"sharedMessage\":null,\"data\":\"{}\",\"attachments\":null,"
      + "\"user\":{\"userId\":123,\"firstName\":\"firstName\",\"lastName\":\"lastName\","
      + "\"displayName\":\"displayName\",\"email\":\"email@email.com\",\"username\":\"username\"},"
      + "\"stream\":{\"streamId\":\"STREAM_ID\",\"streamType\":\"IM\",\"roomName\":null,\"members\":null,"
      + "\"external\":null,\"crossPod\":null},\"externalRecipients\":false,\"diagnostic\":null,"
      + "\"userAgent\":\"AGENT_USER\",\"originalFormat\":\"com.symphony.messageml.v2\","
      + "\"disclaimer\":null,\"sid\":\"SID\",\"replacing\":null,\"replacedBy\":null,"
      + "\"initialTimestamp\":0,\"initialMessageId\":null,\"silent\":null}},\"args\":{\"eventName\":\"EVENT_NAME\"}}";

  @Test
  void executeTest() {
    final String eventName = "EVENT_NAME";
    final String eventId = "EVENT_ID";
    final String executionId = "EXEC_ID";
    final String processDefKey = "PROC_DEF_KEY";
    HistoricVariableUpdateEventEntity historyEvent = new HistoricVariableUpdateEventEntity();
    historyEvent.setProcessDefinitionKey(processDefKey);
    historyEvent.setVariableName(ActivityExecutorContext.EVENT);
    historyEvent.setByteValue(EVENT_HOLDER_STRING.getBytes(StandardCharsets.UTF_8));
    historyEvent.setExecutionId(executionId);

    WorkflowNode workflowNode = new WorkflowNode();
    workflowNode.setId(eventName);
    workflowNode.setEventId(eventId);

    WorkflowDirectedGraph workflowDirectedGraph = new WorkflowDirectedGraph(processDefKey);
    workflowDirectedGraph.registerToDictionary(eventName, workflowNode);
    when(cachingService.getDirectedGraph(anyString())).thenReturn(workflowDirectedGraph);

    doNothing().when(runtimeService).setVariable(anyString(), anyString(), any());

    eventVariableAction.execute(historyEvent);

    verify(cachingService).getDirectedGraph(eq(processDefKey));
    verify(runtimeService).setVariable(eq(executionId), eq(eventId), any(EventHolder.class));
  }

  @Test
  void executeTestProcessingExceptionNotThrown() {
    HistoricVariableUpdateEventEntity historyEvent = new HistoricVariableUpdateEventEntity();
    historyEvent.setVariableName(ActivityExecutorContext.EVENT);
    historyEvent.setByteValue(new byte[] {});

    verify(cachingService, never()).getDirectedGraph(anyString());
    verify(runtimeService, never()).setVariable(anyString(), anyString(), any(EventHolder.class));
  }
}
