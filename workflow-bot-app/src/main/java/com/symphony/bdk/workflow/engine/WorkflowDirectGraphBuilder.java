package com.symphony.bdk.workflow.engine;

import static com.symphony.bdk.workflow.engine.camunda.bpmn.builder.WorkflowNodeBpmnBuilder.DEFAULT_FORM_REPLIED_EVENT_TIMEOUT;

import com.symphony.bdk.workflow.engine.WorkflowDirectGraph.Gateway;
import com.symphony.bdk.workflow.engine.camunda.WorkflowEventToCamundaEvent;
import com.symphony.bdk.workflow.swadl.exception.ActivityNotFoundException;
import com.symphony.bdk.workflow.swadl.exception.InvalidActivityException;
import com.symphony.bdk.workflow.swadl.exception.NoStartingEventException;
import com.symphony.bdk.workflow.swadl.v1.Activity;
import com.symphony.bdk.workflow.swadl.v1.Event;
import com.symphony.bdk.workflow.swadl.v1.EventWithTimeout;
import com.symphony.bdk.workflow.swadl.v1.Workflow;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;
import com.symphony.bdk.workflow.swadl.v1.activity.RelationalEvents;
import com.symphony.bdk.workflow.swadl.v1.event.ActivityCompletedEvent;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * Builder constructs the given workflow into a direct graph structure
 *
 * @see WorkflowDirectGraph
 */
public class WorkflowDirectGraphBuilder {
  private static final String TIMEOUT_SUFFIX = "_timeout";
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
      RelationalEvents onEvents = activity.getEvents();
      computeStandaloneActivities(activities, directGraph, i, activity, activityId, onEvents);
      computeEvents(i, activityId, onEvents, activities, directGraph);
    }
    return directGraph;
  }

  private void computeStandaloneActivities(List<Activity> activities, WorkflowDirectGraph directGraph, int index,
      Activity activity, String activityId, RelationalEvents onEvents) {
    if (onEvents.isEmpty()) {
      if (index == 0) {
        throw new NoStartingEventException(workflow.getId());
      } else {
        directGraph.addChildTo(activities.get(index - 1).getActivity().getId()).addChild(activityId);
        directGraph.addParent(activityId, activities.get(index - 1).getActivity().getId());
        if (activity.getActivity().getIfCondition() != null) {
          directGraph.readWorkflowNode(activityId)
              .addIfCondition(activities.get(index - 1).getActivity().getId(), activity.getActivity().getIfCondition());
        } else if (activity.getActivity().getElseCondition() != null) {
          throw new InvalidActivityException(workflow.getId(),
              "Expecting \"if\" keyword to open a new conditional branching, got \"else\"");
        }
      }
    }
  }

  private void computeEvents(int activityIndex, String activityId, RelationalEvents onEvents, List<Activity> activities,
      WorkflowDirectGraph directGraph) {
    for (Event event : onEvents.getEvents()) {
      String eventNodeId = "";
      if (isTimerFiredEvent(event)) {
        eventNodeId = eventMapper.toTimerFiredEventName(event.getTimerFired());
        computeActivity(activityIndex, activities, eventNodeId, event, onEvents.isExclusive(), directGraph);
        directGraph.registerToDictionary(eventNodeId,
            new WorkflowNode().id(eventNodeId).event(event).elementType(WorkflowNodeType.TIMER_FIRED_EVENT));
      } else {
        Optional<String> signalName = eventMapper.toSignalName(event, workflow);
        if (signalName.isPresent()) {
          eventNodeId = signalName.get();
          computeActivity(activityIndex, activities, eventNodeId, event, onEvents.isExclusive(), directGraph);
          computeSignal(directGraph, event, eventNodeId, activityIndex, activityId, activities);
        } else if (event.getActivityExpired() != null) {
          eventNodeId = computeExpiredActivity(event, activityId, directGraph);
        } else if (event.getActivityFailed() != null) {
          eventNodeId = event.getActivityFailed().getActivityId();
          validateNodeId(eventNodeId, activityId);
          directGraph.readWorkflowNode(activityId).setElementType(WorkflowNodeType.ACTIVITY_FAILED_EVENT);
        } else if (event.getActivityCompleted() != null) {
          eventNodeId = event.getActivityCompleted().getActivityId();
          validateNodeId(eventNodeId, activityId);
          directGraph.readWorkflowNode(eventNodeId).setElementType(WorkflowNodeType.ACTIVITY_COMPLETED_EVENT);
          BaseActivity currentActivity = activities.get(activityIndex).getActivity();
          Optional<String> condition = retrieveCondition(event.getActivityCompleted(), currentActivity, directGraph);
          String finalNodeId = eventNodeId;
          condition.ifPresent(c -> directGraph.readWorkflowNode(activityId).addIfCondition(finalNodeId, c));
        }
      }
      directGraph.addChildTo(eventNodeId).addChild(activityId);
      directGraph.addParent(activityId, eventNodeId);
    }
  }

  private Optional<String> retrieveCondition(ActivityCompletedEvent event, BaseActivity activity,
      WorkflowDirectGraph directGraph) {
    return event.getIfCondition() != null ? Optional.of(event.getIfCondition())
        : Optional.ofNullable(activity.getIfCondition());
  }

  private void validateNodeId(String currentNodeId, String activityId) {
    boolean activityUnknown = workflow.getActivities().stream()
        .noneMatch(a -> a.getActivity().getId().equals(currentNodeId));
    if (activityUnknown) {
      throw new ActivityNotFoundException(workflow.getId(), currentNodeId, activityId);
    }
  }

  private void computeSignal(WorkflowDirectGraph directGraph, Event event, String eventNodeId, int activityIndex,
      String activityId, List<Activity> activities) {
    WorkflowNode signalEvent = new WorkflowNode().id(eventNodeId).event(event);
    if (isFormRepliedEvent(event)) {
      validateNodeId(eventNodeId.substring(WorkflowEventToCamundaEvent.FORM_REPLY_PREFIX.length()), activityId);
      if (event instanceof EventWithTimeout && StringUtils.isEmpty(((EventWithTimeout) event).getTimeout())) {
        ((EventWithTimeout) event).setTimeout(DEFAULT_FORM_REPLIED_EVENT_TIMEOUT);
      }
      directGraph.registerToDictionary(eventNodeId, signalEvent.elementType(WorkflowNodeType.FORM_REPLIED_EVENT));
    } else {
      Activity activity = activities.get(activityIndex);
      String timeout = activity.getActivity().getOn().getTimeout(); // timeout value from on event
      if ((event instanceof EventWithTimeout && StringUtils.isNotEmpty(((EventWithTimeout) event).getTimeout()))
          // timeout value from activity itself
          || StringUtils.isNotEmpty(timeout)) {
        timeout = timeout == null ? ((EventWithTimeout) event).getTimeout() : timeout;
        // in case of oneOf events list, there might be a timeout setup at 'on' level, this timeout will be applied to
        // every event in the onfOf list, check if it is already registered, avoid repeating multiple of them
        String newTimeoutEventId = eventNodeId + TIMEOUT_SUFFIX;
        String parentId = directGraph.getParents(eventNodeId).get(0);
        registerTimeoutEvent(directGraph, newTimeoutEventId, parentId, timeout);
      }
      directGraph.registerToDictionary(eventNodeId, signalEvent.elementType(WorkflowNodeType.SIGNAL_EVENT));
    }
  }

  private String computeExpiredActivity(Event event, String activityId, WorkflowDirectGraph directGraph) {
    // get the parent timeout event of the referred activity
    String parentActivity = directGraph.getParents(event.getActivityExpired().getActivityId()).get(0);
    String grandParentId = directGraph.getParents(parentActivity).get(0);

    WorkflowNode parentNode = directGraph.readWorkflowNode(parentActivity);
    if (parentNode.getElementType() == WorkflowNodeType.FORM_REPLIED_EVENT) {
      directGraph.readWorkflowNode(activityId).setElementType(WorkflowNodeType.ACTIVITY_EXPIRED_EVENT);
      return parentActivity;
    } else {
      String newTimeoutEvent = parentActivity + TIMEOUT_SUFFIX;
      String timeout = ((EventWithTimeout) parentNode.getEvent()).getTimeout();
      registerTimeoutEvent(directGraph, newTimeoutEvent, grandParentId,
          Optional.ofNullable(timeout).orElse(DEFAULT_FORM_REPLIED_EVENT_TIMEOUT));
      return newTimeoutEvent;
    }
  }

  private void registerTimeoutEvent(WorkflowDirectGraph directGraph, String timeoutEventId, String parentId,
      String timeoutValue) {
    if (!directGraph.isRegistered(timeoutEventId)) {
      EventWithTimeout timeoutEvent = new EventWithTimeout();
      timeoutEvent.setTimeout(timeoutValue);
      directGraph.registerToDictionary(timeoutEventId, new WorkflowNode().id(timeoutEventId)
          .event(timeoutEvent)
          .elementType(WorkflowNodeType.ACTIVITY_EXPIRED_EVENT));
      directGraph.addParent(timeoutEventId, parentId);
      directGraph.addChildTo(parentId).gateway(Gateway.EVENT_BASED).addChild(timeoutEventId);
    }
  }

  private void computeActivity(int activityIndex, List<Activity> activities, String nodeId, Event event,
      boolean isExclusive,
      WorkflowDirectGraph directGraph) {
    if (activityIndex == 0) {
      validateActivity(activityIndex, activities, event);
      directGraph.addStartEvent(nodeId);
      directGraph.addParent(nodeId, nodeId);
    } else {
      directGraph.addChildTo(activities.get(activityIndex - 1).getActivity().getId())
          .gateway(isExclusive ? Gateway.EVENT_BASED : Gateway.PARALLEL)
          .addChild(nodeId);
      directGraph.addParent(nodeId, activities.get(activityIndex - 1).getActivity().getId());
    }
  }

  private void validateActivity(int activityIndex, List<Activity> activities, Event event) {
    BaseActivity activity = activities.get(activityIndex).getActivity();
    if (isConditional(activity)) {
      throw new InvalidActivityException(workflow.getId(),
          String.format("Starting activity %s cannot have a conditional branching", activity.getId()));
    }
    if (event instanceof EventWithTimeout && StringUtils.isNotEmpty(((EventWithTimeout) event).getTimeout())) {
      throw new InvalidActivityException(workflow.getId(),
          String.format("Workflow's starting activity %s should not have timeout",
              activity.getId()));
    }
  }

  private boolean isConditional(BaseActivity activity) {
    return activity.getIfCondition() != null || (activity.getOn() != null
        && activity.getOn().getActivityCompleted() != null
        && activity.getOn().getActivityCompleted().getIfCondition() != null);
  }

  private boolean isTimerFiredEvent(Event event) {
    return event.getTimerFired() != null;
  }

  private boolean isFormRepliedEvent(Event event) {
    return event.getFormReplied() != null;
  }

}
