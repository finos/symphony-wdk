package com.symphony.bdk.workflow.engine;

import static com.symphony.bdk.workflow.WorkflowValidator.validateActivityCompletedNodeId;
import static com.symphony.bdk.workflow.WorkflowValidator.validateExistingNodeId;
import static com.symphony.bdk.workflow.WorkflowValidator.validateFirstActivity;
import static com.symphony.bdk.workflow.engine.camunda.bpmn.builder.WorkflowNodeBpmnBuilder.DEFAULT_FORM_REPLIED_EVENT_TIMEOUT;

import com.symphony.bdk.workflow.engine.WorkflowDirectGraph.Gateway;
import com.symphony.bdk.workflow.engine.camunda.WorkflowEventToCamundaEvent;
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
  private static final String JOIN_GATEWAY = "_join_gateway";
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
      computeStandaloneActivities(activities, directGraph, i);
      String activityId = computeParallelJoinGateway(directGraph, activity);
      computeEvents(i, activityId, activities, directGraph);
    }
    return directGraph;
  }

  /**
   * when events are in a "allOf" list, a join gateway is added to ease the final workflow instance construction.
   */
  private String computeParallelJoinGateway(WorkflowDirectGraph directGraph, Activity activity) {
    String activityId = activity.getActivity().getId();
    if (activity.getEvents().isParallel()) {
      String joinActivityId = activityId + JOIN_GATEWAY;
      directGraph.registerToDictionary(joinActivityId,
          new WorkflowNode().id(joinActivityId).elementType(WorkflowNodeType.JOIN_ACTIVITY));
      directGraph.getChildren(joinActivityId).addChild(activityId);
      directGraph.addParent(activityId, joinActivityId);
      return joinActivityId;
    }
    return activityId;
  }

  private void computeStandaloneActivities(List<Activity> activities, WorkflowDirectGraph directGraph, int index) {
    Activity activity = activities.get(index);
    if (activity.getEvents().isEmpty()) {
      if (index == 0) {
        throw new NoStartingEventException(workflow.getId());
      } else {
        String activityId = activity.getActivity().getId();
        directGraph.getChildren(activities.get(index - 1).getActivity().getId()).addChild(activityId);
        directGraph.addParent(activityId, activities.get(index - 1).getActivity().getId());
        if (activity.getActivity().getIfCondition() != null) {
          // add condition to the parent node instead of child node, because
          // - the condition must be created as an exclusive gateway in between the parent and child nodes
          // - in the DFS algo, we always pass through the parent node first, then the child, if we put the condition
          // on child, we first have to go back to parent node to expand the condition using the gateway, then link
          // the gateway to the current child node.
          directGraph.readWorkflowNode(activityId)
              .addIfCondition(activities.get(index - 1).getActivity().getId(), activity.getActivity().getIfCondition());
        } else if (activity.getActivity().getElseCondition() != null) {
          throw new InvalidActivityException(workflow.getId(),
              "Expecting \"if\" keyword to open a new conditional branching, got \"else\"");
        }
      }
    }
  }

  private void computeEvents(int activityIndex, String activityId, List<Activity> activities,
      WorkflowDirectGraph directGraph) {
    Activity activity = activities.get(activityIndex);
    RelationalEvents onEvents = activity.getEvents();

    for (Event event : onEvents.getEvents()) {
      String eventNodeId = "";
      if (isTimerFiredEvent(event)) {
        eventNodeId = eventMapper.toTimerFiredEventName(event.getTimerFired());
        computeActivity(activityIndex, activities, eventNodeId, event, onEvents, directGraph);
        directGraph.registerToDictionary(eventNodeId,
            new WorkflowNode().id(eventNodeId).event(event).elementType(WorkflowNodeType.TIMER_FIRED_EVENT));
      } else {
        Optional<String> signalName = eventMapper.toSignalName(event, workflow);
        if (signalName.isPresent()) {
          eventNodeId = signalName.get();
          computeActivity(activityIndex, activities, eventNodeId, event, onEvents, directGraph);
          computeSignal(directGraph, event, eventNodeId, activityIndex, activities);
        } else if (event.getActivityExpired() != null) {
          eventNodeId = computeExpiredActivity(event, activity.getActivity().getId(), directGraph);
        } else if (event.getActivityFailed() != null) {
          eventNodeId = event.getActivityFailed().getActivityId();
          validateExistingNodeId(eventNodeId, activity.getActivity().getId(), workflow.getId(), directGraph);
          directGraph.readWorkflowNode(activity.getActivity().getId())
              .setElementType(WorkflowNodeType.ACTIVITY_FAILED_EVENT);
        } else if (event.getActivityCompleted() != null) {
          eventNodeId = event.getActivityCompleted().getActivityId();
          validateActivityCompletedNodeId(eventNodeId, activityId, workflow);
          directGraph.readWorkflowNode(eventNodeId).setElementType(WorkflowNodeType.ACTIVITY_COMPLETED_EVENT);
          BaseActivity currentActivity = activity.getActivity();
          Optional<String> condition = retrieveCondition(event.getActivityCompleted(), currentActivity);
          String finalNodeId = eventNodeId;
          condition.ifPresent(c -> directGraph.readWorkflowNode(activityId).addIfCondition(finalNodeId, c));
        }
      }
      if (!(onEvents.isParallel() && (event.getActivityCompleted() != null || event.getActivityFailed() != null
          || event.getActivityExpired() != null))) {
        directGraph.getChildren(eventNodeId).addChild(activityId);
        directGraph.addParent(activityId, eventNodeId);
      }
    }
  }

  private Optional<String> retrieveCondition(ActivityCompletedEvent event, BaseActivity activity) {
    return event.getIfCondition() != null ? Optional.of(event.getIfCondition())
        : Optional.ofNullable(activity.getIfCondition());
  }

  private void computeSignal(WorkflowDirectGraph directGraph, Event event, String eventNodeId, int activityIndex,
      List<Activity> activities) {
    WorkflowNode signalEvent = new WorkflowNode().id(eventNodeId).event(event);
    String activityId = activities.get(activityIndex).getActivity().getId();
    if (isFormRepliedEvent(event)) {
      validateExistingNodeId(eventNodeId.substring(WorkflowEventToCamundaEvent.FORM_REPLY_PREFIX.length()), activityId,
          workflow.getId(), directGraph);
      if (event instanceof EventWithTimeout && StringUtils.isEmpty(((EventWithTimeout) event).getTimeout())) {
        ((EventWithTimeout) event).setTimeout(DEFAULT_FORM_REPLIED_EVENT_TIMEOUT);
      }
      directGraph.registerToDictionary(eventNodeId, signalEvent.elementType(WorkflowNodeType.FORM_REPLIED_EVENT));
    } else {
      Activity activity = activities.get(activityIndex);
      String timeout = activity.getActivity().getOn().getTimeout(); // timeout value from on event, never nullable
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
    validateExistingNodeId(parentActivity, activityId, workflow.getId(), directGraph);
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
      directGraph.getChildren(parentId).gateway(Gateway.EVENT_BASED).addChild(timeoutEventId);
    }
  }

  private void computeActivity(int activityIndex, List<Activity> activities, String nodeId, Event event,
      RelationalEvents onEvents, WorkflowDirectGraph directGraph) {
    if (activityIndex == 0) {
      validateFirstActivity(activities.get(0).getActivity(), event, workflow.getId());
      directGraph.addStartEvent(nodeId);
    } else {
      boolean isParallel = onEvents.isParallel();
      String allOfEventParentId = onEvents.getParentId();
      String parentId = isParallel && StringUtils.isNotEmpty(allOfEventParentId) ? allOfEventParentId
          : activities.get(activityIndex - 1).getActivity().getId();
      directGraph.getChildren(parentId).gateway(isParallel ? Gateway.PARALLEL : Gateway.EVENT_BASED).addChild(nodeId);
      directGraph.addParent(nodeId, parentId);
    }
  }


  private boolean isTimerFiredEvent(Event event) {
    return event.getTimerFired() != null;
  }

  private boolean isFormRepliedEvent(Event event) {
    return event.getFormReplied() != null;
  }

}
