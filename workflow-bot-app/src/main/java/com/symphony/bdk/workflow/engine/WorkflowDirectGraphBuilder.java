package com.symphony.bdk.workflow.engine;

import com.symphony.bdk.workflow.engine.WorkflowDirectGraph.Gateway;
import com.symphony.bdk.workflow.engine.camunda.WorkflowEventToCamundaEvent;
import com.symphony.bdk.workflow.swadl.exception.ActivityNotFoundException;
import com.symphony.bdk.workflow.swadl.exception.NoStartingEventException;
import com.symphony.bdk.workflow.swadl.v1.Activity;
import com.symphony.bdk.workflow.swadl.v1.Event;
import com.symphony.bdk.workflow.swadl.v1.EventWithTimeout;
import com.symphony.bdk.workflow.swadl.v1.Workflow;
import com.symphony.bdk.workflow.swadl.v1.activity.RelationalEvents;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

public class WorkflowDirectGraphBuilder {
  private static final String DEFAULT_FORM_REPLIED_EVENT_TIMEOUT = "PT24H";
  /**
   * element id is key, element parent id is value
   */
  private final Workflow workflow;
  private final WorkflowEventToCamundaEvent eventMapper;

  public WorkflowDirectGraphBuilder(Workflow workflow, WorkflowEventToCamundaEvent eventMapper) {
    this.workflow = workflow;
    this.eventMapper = eventMapper;
  }

  public WorkflowDirectGraph build() {
    List<Activity> activities = workflow.getActivities();
    WorkflowDirectGraph directGraph = new WorkflowDirectGraph();
    activities.forEach(activity -> directGraph.registerToDictionary(activity.getActivity().getId(),
        new WorkflowNode().id(activity.getActivity().getId())
            .activity(activity.getActivity())
            .elementType(WorkflowNodeType.ACTIVITY)));

    for (int i = 0; i < activities.size(); i++) {
      Activity activity = activities.get(i);
      String activityId = activity.getActivity().getId();
      RelationalEvents onEvents = getOnEvents(Optional.of(activity));

      if (onEvents.isEmpty()) {
        if (i == 0) {
          throw new NoStartingEventException(workflow.getId());
        } else {
          directGraph.addChildTo(activities.get(i - 1).getActivity().getId()).addChild(activityId);
          directGraph.addParent(activityId, activities.get(i - 1).getActivity().getId());
          if (activity.getActivity().getIfCondition() != null) {
            directGraph.readWorkflowNode(activityId)
                .addIfCondition(activities.get(i - 1).getActivity().getId(), activity.getActivity().getIfCondition());
          }
        }
      }
      computeEvents(i, activityId, onEvents, activities, directGraph);
    }
    return directGraph;
  }

  private void computeEvents(int activityIndex, String activityId, RelationalEvents onEvents,
      List<Activity> activities, WorkflowDirectGraph directGraph) {
    for (Event event : onEvents.getEvents()) {
      String nodeId = "";
      if (isTimerFiredEvent(event)) {
        nodeId = eventMapper.toTimerFiredEventName(event.getTimerFired());
        computeFirstActivity(activityIndex, activities, nodeId, onEvents.isExclusive(), directGraph);
        directGraph.registerToDictionary(nodeId,
            new WorkflowNode().id(nodeId).event(event).elementType(WorkflowNodeType.TIMER_FIRED_EVENT));
      } else {
        Optional<String> signalName = eventMapper.toSignalName(event, workflow);
        if (signalName.isPresent()) {
          nodeId =
              computeSignal(activityIndex, onEvents.isExclusive(), activities, directGraph, event, signalName.get());
        } else if (event.getActivityExpired() != null) {
          nodeId = computeExpiredActivity(event, activityId, directGraph);
        } else if (event.getActivityFailed() != null) {
          nodeId = event.getActivityFailed().getActivityId();
          validateNodeId(nodeId, activityId);
          directGraph.readWorkflowNode(activityId).setElementType(WorkflowNodeType.ACTIVITY_FAILED_EVENT);
        } else if (event.getActivityCompleted() != null) {
          nodeId = event.getActivityCompleted().getActivityId();
          validateNodeId(nodeId, activityId);
          directGraph.readWorkflowNode(nodeId).setElementType(WorkflowNodeType.ACTIVITY_COMPLETED_EVENT);
          if (event.getActivityCompleted().getIfCondition() != null) {
            directGraph.readWorkflowNode(activityId)
                .addIfCondition(nodeId, event.getActivityCompleted().getIfCondition());
          }
        }
      }
      directGraph.addChildTo(nodeId)
          .gateway(onEvents.isExclusive() ? Gateway.EXCLUSIVE : Gateway.PARALLEL)
          .addChild(activityId);
      directGraph.addParent(activityId, nodeId);
    }
  }

  private void validateNodeId(String currentNodeId, String activityId) {
    boolean activityUnknown = workflow.getActivities().stream()
        .noneMatch(a -> a.getActivity().getId().equals(currentNodeId));
    if (activityUnknown) {
      throw new ActivityNotFoundException(workflow.getId(), currentNodeId, activityId);
    }
  }

  private String computeSignal(int activityIndex, boolean isExclusive, List<Activity> activities,
      WorkflowDirectGraph directGraph, Event event, String nodeId) {
    computeFirstActivity(activityIndex, activities, nodeId, isExclusive, directGraph);
    WorkflowNode signalEvent = new WorkflowNode().id(nodeId).event(event);
    if (isFormRepliedEvent(event)) {
      validateNodeId(nodeId.substring(WorkflowEventToCamundaEvent.FORM_REPLY_PREFIX.length()), activities.get(activityIndex).getActivity().getId());
      if (event instanceof EventWithTimeout && StringUtils.isEmpty(((EventWithTimeout) event).getTimeout())) {
        ((EventWithTimeout) event).setTimeout(DEFAULT_FORM_REPLIED_EVENT_TIMEOUT);
      }
      directGraph.registerToDictionary(nodeId, signalEvent.elementType(WorkflowNodeType.FORM_REPLIED_EVENT));
    } else {
      directGraph.registerToDictionary(nodeId, signalEvent.elementType(WorkflowNodeType.SIGNAL_EVENT));
    }
    return nodeId;
  }

  private String computeExpiredActivity(Event event, String activityId, WorkflowDirectGraph directGraph) {
    // get the parent timeout event of the referred activity
    String parentActivity = directGraph.getParents(event.getActivityExpired().getActivityId()).get(0);
    String grandParentId = directGraph.getParents(parentActivity).get(0);

    WorkflowNode parentNode = directGraph.readWorkflowNode(parentActivity);
    if (parentNode.getElementType() == WorkflowNodeType.FORM_REPLIED_EVENT && !parentNode.getEvent()
        .getFormReplied()
        .getExclusive()) {
      directGraph.readWorkflowNode(activityId).setElementType(WorkflowNodeType.ACTIVITY_EXPIRED_EVENT);
      return parentActivity;
    } else {
      String timeout = ((EventWithTimeout) parentNode.getEvent()).getTimeout();
      timeout = timeout == null ? DEFAULT_FORM_REPLIED_EVENT_TIMEOUT : timeout;
      String newTimeoutEvent = parentActivity + "_timeout";

      EventWithTimeout timeoutEvent = new EventWithTimeout();
      timeoutEvent.setTimeout(timeout);
      directGraph.registerToDictionary(newTimeoutEvent, new WorkflowNode().id(newTimeoutEvent)
          .event(timeoutEvent)
          .elementType(WorkflowNodeType.ACTIVITY_EXPIRED_EVENT));
      directGraph.addParent(newTimeoutEvent, grandParentId);

      directGraph.addChildTo(grandParentId).gateway(Gateway.EVENT_BASED).addChild(newTimeoutEvent);
      return newTimeoutEvent;
    }
  }

  private void computeFirstActivity(int activityIndex, List<Activity> activities, String nodeId, boolean isExclusive,
      WorkflowDirectGraph directGraph) {
    if (activityIndex == 0) {
      directGraph.addStartEvent(nodeId);
      directGraph.addParent(nodeId, nodeId);
    } else {
      directGraph.addChildTo(activities.get(activityIndex - 1).getActivity().getId())
          .gateway(isExclusive ? Gateway.EXCLUSIVE : Gateway.PARALLEL)
          .addChild(nodeId);
      directGraph.addParent(nodeId, activities.get(activityIndex - 1).getActivity().getId());
    }
  }

  private boolean isTimerFiredEvent(Event event) {
    return event.getTimerFired() != null;
  }

  private boolean isFormRepliedEvent(Event event) {
    return event.getFormReplied() != null;
  }

  private RelationalEvents getOnEvents(Optional<Activity> optionalActivity) {
    return optionalActivity.map(Activity::getEvents).orElseThrow(() -> new NoStartingEventException(workflow.getId()));
  }
}
