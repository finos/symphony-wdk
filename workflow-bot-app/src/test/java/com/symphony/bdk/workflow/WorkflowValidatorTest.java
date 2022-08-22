package com.symphony.bdk.workflow;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.symphony.bdk.workflow.engine.WorkflowDirectGraph;
import com.symphony.bdk.workflow.swadl.exception.ActivityNotFoundException;
import com.symphony.bdk.workflow.swadl.exception.InvalidActivityException;
import com.symphony.bdk.workflow.swadl.v1.Activity;
import com.symphony.bdk.workflow.swadl.v1.Event;
import com.symphony.bdk.workflow.swadl.v1.EventWithTimeout;
import com.symphony.bdk.workflow.swadl.v1.Workflow;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;
import com.symphony.bdk.workflow.swadl.v1.activity.connection.AcceptConnection;
import com.symphony.bdk.workflow.swadl.v1.event.ActivityCompletedEvent;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.stream.Stream;

class WorkflowValidatorTest {

  static Stream<Arguments> firstActivities() {
    final BaseActivity mockActivity1 = mock(BaseActivity.class);
    when(mockActivity1.getIfCondition()).thenReturn("condition");

    final BaseActivity mockActivity2 = mock(BaseActivity.class);
    EventWithTimeout mockEvent1 = mock(EventWithTimeout.class);
    when(mockEvent1.getTimeout()).thenReturn("PT24h");

    final BaseActivity mockActivity3 = mock(BaseActivity.class);
    EventWithTimeout mockEvent2 = mock(EventWithTimeout.class);
    when(mockEvent2.getTimeout()).thenReturn("PT24h");
    when(mockActivity3.getOn()).thenReturn(mockEvent2);
    when(mockEvent2.getActivityCompleted()).thenReturn(mock(ActivityCompletedEvent.class));

    return Stream.of(
        arguments(mockActivity1, null, "workflow"),
        arguments(mockActivity2, mockEvent1, "workflow"),
        arguments(mockActivity3, mockEvent2, "workflow")
    );
  }

  @ParameterizedTest
  @MethodSource("firstActivities")
  void validateFirstActivity(BaseActivity activity, Event event, String workflowId) {
    Assertions.assertThatThrownBy(
        () -> WorkflowValidator.validateFirstActivity(activity, event, workflowId)).isInstanceOf(
        InvalidActivityException.class);
  }

  @Test
  void validateActivityCompletedNodeId() {
    Workflow workflow = new Workflow();
    AcceptConnection accept = new AcceptConnection();
    accept.setId("accept");
    Activity activity = new Activity();
    activity.setImplementation(accept);
    workflow.setActivities(Collections.singletonList(activity));
    Assertions.assertThatThrownBy(
        () -> WorkflowValidator.validateActivityCompletedNodeId("unknownId", "activity", workflow)).isInstanceOf(
        ActivityNotFoundException.class);
  }

  @Test
  void validateExistingNodeId() {
    WorkflowDirectGraph graph = new WorkflowDirectGraph();
    graph.addParent("activity", "parent");
    Assertions.assertThatThrownBy(
        () -> WorkflowValidator.validateExistingNodeId("unknownId", "activity", "workflowId", graph)).isInstanceOf(
        ActivityNotFoundException.class);
  }
}
