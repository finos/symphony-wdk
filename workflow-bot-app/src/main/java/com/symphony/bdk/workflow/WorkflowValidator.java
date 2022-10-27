package com.symphony.bdk.workflow;

import com.symphony.bdk.workflow.engine.WorkflowDirectGraph;
import com.symphony.bdk.workflow.swadl.exception.ActivityNotFoundException;
import com.symphony.bdk.workflow.swadl.exception.InvalidActivityException;
import com.symphony.bdk.workflow.swadl.v1.Event;
import com.symphony.bdk.workflow.swadl.v1.EventWithTimeout;
import com.symphony.bdk.workflow.swadl.v1.Workflow;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class WorkflowValidator {

  public static void validateFirstActivity(BaseActivity activity, Event event, String workflowId) {
    if (event instanceof EventWithTimeout && StringUtils.isNotEmpty(((EventWithTimeout) event).getTimeout())) {
      throw new InvalidActivityException(workflowId,
          String.format("Workflow's starting activity %s must not have timeout", activity.getId()));
    }
    // the given event might be a event from oneOf list, if so the following checked activities are acceptable.
    // here we want to forbidden having these activities from "on" section only
    EventWithTimeout eventWithTimeout = activity.getOn();
    if (eventWithTimeout != null && (eventWithTimeout.getActivityCompleted() != null
        || eventWithTimeout.getActivityFailed() != null
        || eventWithTimeout.getActivityExpired() != null)) {
      throw new InvalidActivityException(workflowId,
          String.format("Workflow's starting activity %s must not be dependent on other activities",
              activity.getId()));
    }
  }

  public static void validateActivityCompletedNodeId(String currentNodeId, String activityId, Workflow workflow) {
    boolean activityUnknown = workflow.getActivities().stream()
        .noneMatch(a -> a.getActivity().getId().equals(currentNodeId));
    if (activityUnknown) {
      throw new ActivityNotFoundException(workflow.getId(), currentNodeId, activityId);
    }
  }

  public static void validateExistingNodeId(String currentNodeId, String activityId, String workflowId,
      WorkflowDirectGraph graph) {
    if (!graph.hasSeenBefore(currentNodeId)) {
      throw new ActivityNotFoundException(workflowId, currentNodeId, activityId);
    }
  }
}
