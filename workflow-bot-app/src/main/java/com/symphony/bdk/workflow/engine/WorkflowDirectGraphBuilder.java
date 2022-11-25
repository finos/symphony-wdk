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
import com.symphony.bdk.workflow.swadl.v1.event.ActivityExpiredEvent;
import com.symphony.bdk.workflow.swadl.v1.event.TimerFiredEvent;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

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
        new WorkflowNode().id(activity.getActivity().getId()).eventId(activity.getActivity().getId())
            .wrappedType(activity.getActivity().getClass())
            .activity(activity.getActivity())
            .elementType(WorkflowNodeType.ACTIVITY)));

    for (int i = 0; i < activities.size(); i++) {
      Activity activity = activities.get(i);
      computeStandaloneActivities(activities, directGraph, i);
      String activityId = computeParallelJoinGateway(directGraph, activity);
      computeEvents(i, activityId, activities, directGraph);
    }
    directGraph.getVariables().putAll(workflow.getVariables());
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
          new WorkflowNode().id(joinActivityId)
              .eventId(joinActivityId)
              .wrappedType(JoinGateway.class)
              .elementType(WorkflowNodeType.JOIN_ACTIVITY));
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
            new WorkflowNode().id(eventNodeId)
                .eventId(eventNodeId)
                .wrappedType(TimerFiredEvent.class)
                .event(event)
                .elementType(WorkflowNodeType.TIMER_FIRED_EVENT));
      } else {
        Optional<Triple<String, String, Class<?>>> signalName = eventMapper.toSignalName(event, workflow);
        if (signalName.isPresent()) {
          Triple<String, String, Class<?>> triple = signalName.get();
          eventNodeId = triple.getMiddle();
          if (activity.getActivity() != null && StringUtils.isNotBlank(activity.getActivity().getIfCondition())) {
            directGraph.readWorkflowNode(activityId)
                .addIfCondition(eventNodeId, activity.getActivity().getIfCondition());
          }
          computeActivity(activityIndex, activities, eventNodeId, event, onEvents, directGraph);
          computeSignal(activityIndex, activities, triple, event, onEvents, directGraph);
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
      directGraph.getChildren(eventNodeId).addChild(activityId);
      directGraph.addParent(activityId, eventNodeId);
    }
  }

  private String readEventId(Triple<String, String, Class<?>> triple) {
    return triple.getLeft() == null ? triple.getMiddle() : triple.getLeft();
  }

  private Optional<String> retrieveCondition(ActivityCompletedEvent event, BaseActivity activity) {
    return event.getIfCondition() != null ? Optional.of(event.getIfCondition())
        : Optional.ofNullable(activity.getIfCondition());
  }

  private void computeSignal(int activityIndex, List<Activity> activities, Triple<String, String, Class<?>> eventNodeId,
      Event event, RelationalEvents onEvents, WorkflowDirectGraph directGraph) {
    WorkflowNode signalEvent = new WorkflowNode().id(eventNodeId.getMiddle())
        .eventId(readEventId(eventNodeId))
        .wrappedType(eventNodeId.getRight())
        .event(event);
    String activityId = activities.get(activityIndex).getActivity().getId();
    if (isFormRepliedEvent(event) && !event.getFormReplied().getExclusive()) {
      computeNoExclusiveFormReplyEvent(eventNodeId, event, directGraph, signalEvent, activityId);
    } else {
      Activity activity = activities.get(activityIndex);
      computeActivityTimeout(activityIndex, eventNodeId, event, onEvents, directGraph, signalEvent, activityId,
          activity);
      directGraph.registerToDictionary(signalEvent.getId(), signalEvent);
    }
  }

  private void computeActivityTimeout(int activityIndex, Triple<String, String, Class<?>> eventNodeId, Event event,
      RelationalEvents onEvents, WorkflowDirectGraph directGraph, WorkflowNode signalEvent, String activityId,
      Activity activity) {
    String timeout = activity.getActivity().getOn().getTimeout(); // timeout value from on event, never nullable
    signalEvent.elementType(WorkflowNodeType.SIGNAL_EVENT);

    if (isFormRepliedEvent(event)) {
      signalEvent.elementType(WorkflowNodeType.FORM_REPLIED_EVENT);
      if (activityIndex > 0) {
        validateExistingNodeId(
            eventNodeId.getMiddle().substring(WorkflowEventToCamundaEvent.FORM_REPLY_PREFIX.length()),
            activityId,
            workflow.getId(), directGraph);
        if (!onEvents.isParallel() && StringUtils.isEmpty(timeout)) {
          timeout = DEFAULT_FORM_REPLIED_EVENT_TIMEOUT;
        }
      }
    }
    if ((event instanceof EventWithTimeout && StringUtils.isNotEmpty(((EventWithTimeout) event).getTimeout()))
        // timeout value from activity itself
        || StringUtils.isNotEmpty(timeout)) {
      timeout = timeout == null ? ((EventWithTimeout) event).getTimeout() : timeout;
      // in case of oneOf events list, there might be a timeout setup at 'on' level, this timeout will be applied to
      // every event in the onfOf list, check if it is already registered, avoid repeating multiple of them
      String newTimeoutEventId = signalEvent.getId() + TIMEOUT_SUFFIX;
      String parentId = directGraph.getParents(signalEvent.getId()).get(0);
      registerTimeoutEvent(directGraph, newTimeoutEventId, parentId, timeout);
    }
  }

  private void computeNoExclusiveFormReplyEvent(Triple<String, String, Class<?>> eventNodeId, Event event,
      WorkflowDirectGraph directGraph, WorkflowNode signalEvent, String activityId) {
    validateExistingNodeId(eventNodeId.getMiddle().substring(WorkflowEventToCamundaEvent.FORM_REPLY_PREFIX.length()),
        activityId, workflow.getId(), directGraph);
    if (event instanceof EventWithTimeout && StringUtils.isEmpty(((EventWithTimeout) event).getTimeout())) {
      ((EventWithTimeout) event).setTimeout(DEFAULT_FORM_REPLIED_EVENT_TIMEOUT);
    }
    directGraph.registerToDictionary(signalEvent.getId(), signalEvent.elementType(WorkflowNodeType.FORM_REPLIED_EVENT));
  }

  private String computeExpiredActivity(Event event, String activityId, WorkflowDirectGraph directGraph) {
    // get the parent timeout event of the referred activity
    String parentActivity = directGraph.getParents(event.getActivityExpired().getActivityId()).get(0);
    validateExistingNodeId(parentActivity, activityId, workflow.getId(), directGraph);
    String grandParentId = directGraph.getParents(parentActivity).get(0);

    WorkflowNode parentNode = directGraph.readWorkflowNode(parentActivity);
    if (parentNode.isNotExclusiveFormReply()) {
      directGraph.readWorkflowNode(activityId).setElementType(WorkflowNodeType.ACTIVITY_EXPIRED_EVENT);
      directGraph.readWorkflowNode(activityId).setWrappedType(ActivityExpiredEvent.class);
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
      timeoutEvent.setActivityExpired(new ActivityExpiredEvent());
      directGraph.registerToDictionary(timeoutEventId, new WorkflowNode().id(timeoutEventId).eventId(timeoutEventId)
          .event(timeoutEvent)
          .elementType(WorkflowNodeType.ACTIVITY_EXPIRED_EVENT)
          .wrappedType(ActivityExpiredEvent.class));
      directGraph.addParent(timeoutEventId, parentId);
      directGraph.getChildren(parentId).gateway(Gateway.EVENT_BASED).addChild(timeoutEventId);
    }
  }

  private void computeActivity(int activityIndex, List<Activity> activities, String nodeId, Event event,
      RelationalEvents onEvents, WorkflowDirectGraph directGraph) {
    if (activityIndex == 0) {
      validateFirstActivity(activities.get(0).getActivity(), event, workflow.getId());
      directGraph.addStartEvent(nodeId);
    } else if (!directGraph.getParents(activities.get(activityIndex - 1).getActivity().getId())
        .contains(nodeId)) { // the current event node is not a parent of previous activity
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
