package com.symphony.bdk.workflow.engine.camunda.bpmn;

import com.symphony.bdk.workflow.engine.camunda.CamundaExecutor;
import com.symphony.bdk.workflow.engine.camunda.WorkflowEventToCamundaEvent;
import com.symphony.bdk.workflow.engine.camunda.audit.ScriptTaskAuditListener;
import com.symphony.bdk.workflow.engine.camunda.variable.VariablesListener;
import com.symphony.bdk.workflow.swadl.ActivityRegistry;
import com.symphony.bdk.workflow.swadl.exception.ActivityNotFoundException;
import com.symphony.bdk.workflow.swadl.exception.InvalidActivityException;
import com.symphony.bdk.workflow.swadl.exception.NoStartingEventException;
import com.symphony.bdk.workflow.swadl.v1.Activity;
import com.symphony.bdk.workflow.swadl.v1.Event;
import com.symphony.bdk.workflow.swadl.v1.Workflow;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;
import com.symphony.bdk.workflow.swadl.v1.activity.ExecuteScript;
import com.symphony.bdk.workflow.swadl.v1.event.ActivityCompletedEvent;
import com.symphony.bdk.workflow.swadl.v1.event.ActivityFailedEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.DeploymentBuilder;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.AbstractActivityBuilder;
import org.camunda.bpm.model.bpmn.builder.AbstractCatchEventBuilder;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.builder.ExclusiveGatewayBuilder;
import org.camunda.bpm.model.bpmn.builder.IntermediateCatchEventBuilder;
import org.camunda.bpm.model.bpmn.builder.ProcessBuilder;
import org.camunda.bpm.model.bpmn.builder.SubProcessBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Events are created with async before to make sure they are not blocking the dispatch of events (starting or
 * intermediate). This way, 2 workflows listening to the same event are started in parallel
 */
@Slf4j
@Component
public class CamundaBpmnBuilder {
  public static final String DEPLOYMENT_RESOURCE_TOKEN_KEY = "WORKFLOW_TOKEN";
  private static final String DEFAULT_FORM_REPLIED_EVENT_TIMEOUT = "PT24H";


  private final RepositoryService repositoryService;
  private final WorkflowEventToCamundaEvent eventToMessage;

  @Autowired
  public CamundaBpmnBuilder(RepositoryService repositoryService,
      WorkflowEventToCamundaEvent eventToMessage) {
    this.repositoryService = repositoryService;
    this.eventToMessage = eventToMessage;
  }

  public Deployment addWorkflow(Workflow workflow) throws JsonProcessingException {
    BpmnModelInstance instance = workflowToBpmn(workflow);

    try {
      DeploymentBuilder deploymentBuilder = repositoryService.createDeployment()
          .name(workflow.getId())
          .addModelInstance(workflow.getId() + ".bpmn", instance);

      deploymentBuilder = setWorkflowTokenIfExists(deploymentBuilder, workflow);
      return deploymentBuilder.deploy();
    } finally {
      if (log.isDebugEnabled()) {
        WorkflowDebugger.generateDebugFiles(workflow.getId(), instance);
      }
    }
  }

  private DeploymentBuilder setWorkflowTokenIfExists(DeploymentBuilder deploymentBuilder, Workflow workflow) {
    workflow.getActivities().forEach(activity -> {
      Optional<String> token = activity.getEvents()
          .stream()
          .filter(event -> event.getRequestReceived() != null && event.getRequestReceived().getToken() != null)
          .map(event -> event.getRequestReceived().getToken())
          .findFirst();

      token.ifPresent(s -> deploymentBuilder.addString(DEPLOYMENT_RESOURCE_TOKEN_KEY, s));
    });

    return deploymentBuilder;
  }

  private List<Event> getStartingEvents(Workflow workflow) {
    return workflow.getFirstActivity()
        .map(Activity::getEvents)
        .orElseThrow(() -> new NoStartingEventException(workflow.getId()));
  }

  private void checkActivityIsKnown(Workflow workflow, String activityId, String activityIdToCheck) {
    boolean activityUnknown = workflow.getActivities().stream()
        .noneMatch(a -> a.getActivity().getId().equals(activityIdToCheck));
    if (activityUnknown) {
      throw new ActivityNotFoundException(workflow.getId(), activityIdToCheck, activityId);
    }
  }

  private BpmnModelInstance workflowToBpmn(Workflow workflow) throws JsonProcessingException {
    // spaces are not supported in BPMN here
    String processId = workflow.getId().replaceAll("\\s+", "");

    ProcessBuilder process = Bpmn.createExecutableProcess(processId).name(workflow.getId());

    // a workflow starts with at least one named signal event
    List<AbstractFlowNodeBuilder<?, ?>> eventsToConnect = new ArrayList<>();
    AbstractFlowNodeBuilder<?, ?> builder = startingEvents(workflow, process, eventsToConnect);

    // we have to carry a bit of state while processing the activities
    String lastActivity = "";
    Map<String, String> parentActivities = new HashMap<>();

    Map<String, AbstractFlowNodeBuilder<?, ?>> activityExpirations = new HashMap<>();
    List<AbstractFlowNodeBuilder<?, ?>> subProcessedFinish = new ArrayList<>();
    Map<String, AbstractFlowNodeBuilder<?, ?>> gateways = new HashMap<>();
    Map<String, Pair<BaseActivity, Event>> flowsToCreate = new HashMap<>();
    Map<String, AbstractFlowNodeBuilder<?, ?>> activityToTimeoutBuilderMap = new HashMap<>();

    // then each activity is processed
    for (Activity activityContainer : workflow.getActivities()) {
      BaseActivity activity = activityContainer.getActivity();

      builder = addIntermediateEvents(eventsToConnect, builder, lastActivity, activity, workflow);
      if (!onFormRepliedEvent(activity)) {
        if (hasTimeout(activity) && workflow.getFirstActivity().isPresent() && activity.getId()
            .equals(workflow.getFirstActivity().get().getActivity().getId())) {
          throw new InvalidActivityException(workflow.getId(),
              String.format("Workflow's starting activity %s should not have timeout", activity.getId()));
        } else if (hasTimeout(activity) && activity.getOn() != null) {
          setTimeout(activity, activity.getOn().getTimeout(), builder, activityExpirations,
              activityToTimeoutBuilderMap);
        }
      }

      // process events starting the activity
      if (onFormRepliedEvent(activity) && activity.getOn() != null) {
        checkActivityIsKnown(workflow, activity.getId(), activity.getOn().getFormReplied().getFormId());
        builder = formReply(builder, activity, activityExpirations, activityToTimeoutBuilderMap);

        if (!isUniqueReplyForm(activity)) {
          subProcessedFinish.add(builder);
        }
      }

      // activity-failed events are handled as boundary error events
      ActivityFailedEvent firstOnActivityFailedEvent = firstOnActivityFailedEvent(activity);
      if (firstOnActivityFailedEvent != null) {
        AbstractFlowNodeBuilder<?, ?> failedActivity = builder.moveToNode(firstOnActivityFailedEvent.getActivityId());
        if (failedActivity instanceof AbstractActivityBuilder) {
          builder = ((AbstractActivityBuilder<?, ?>) failedActivity).boundaryEvent()
              .name("error_" + activity.getId())
              .error();
        } else {
          throw new InvalidActivityException(workflow.getId(),
              String.format("Could not find activity with id %s referenced by activity-failed event",
                  firstOnActivityFailedEvent.getActivityId()));
        }
      }

      if (onActivityExpired(activity) && activity.getOn() != null) {
        List<String> activitiesToExpireList = getActivityToExpire(activity);
        List<AbstractFlowNodeBuilder<?, ?>> buildersList = new ArrayList<>();
        activitiesToExpireList.forEach(acId -> {
          // add the activity as a form expiration
          AbstractFlowNodeBuilder<?, ?> activityExpirationBuilder =
              activityExpirations.get(acId);

          if (activityExpirationBuilder != null) { // TODO we have to support timeouts when unique=true
            if (activitiesToExpireList.indexOf(acId) == 0) {
              // The first activity of the list is added to the current builder
              try {
                activityExpirationBuilder = addTask(activityExpirationBuilder, activity);
                buildersList.add(activityExpirationBuilder);
              } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
              }
              // update it for the next activities
              activityExpirations.put(activitiesToExpireList.get(0), activityExpirationBuilder);
            } else {
              // The remaining activities in the list should be connected to the given source
              AbstractFlowNodeBuilder<?, ?> sourceBuilder = activityToTimeoutBuilderMap.get(acId);
              this.connectSourceToTarget(activityExpirationBuilder, sourceBuilder.getElement().getId(),
                  activity.getId());
            }
          }
        });
        if (!buildersList.isEmpty()) {
          builder = buildersList.get(0);
        }
      } else {
        // add the activity in the normal flow

        // how is connected with the previous activity, explicit or default?
        List<ActivityCompletedEvent> activityCompletedEvents = getActivityCompletedEvents(activity);
        if (activityCompletedEvents.isEmpty()) {
          // implicit parent activity is the one declared before so the builder will automatically connect it
          parentActivities.put(activity.getId(), lastActivity);
        } else {
          // connect it with its parent activity if we already processed it
          // (loops are dependencies on activities not yet processed)
          if (parentActivities.containsKey(activityCompletedEvents.get(0).getActivityId())) {
            builder = builder.moveToNode(activityCompletedEvents.get(0).getActivityId());
            parentActivities.put(activity.getId(), activityCompletedEvents.get(0).getActivityId());

            // connect the first one, others will be connected once the task is created using connectTo
            activityCompletedEvents.remove(0);
          }
        }

        String parentId = parentActivities.get(activity.getId());

        if (onConditional(activity)) {
          if (gateways.containsKey(parentId)) {
            // we already opened a gateway, so it is an 'else if'
            builder = gateways.get(parentId);
            builder = builder.moveToLastGateway();
          } else {
            // otherwise, it is a new 'if'
            builder = builder.exclusiveGateway();
          }
          builder = builder.condition("if", activity.getIfCondition());
        }

        if (activity.getElseCondition() != null) {
          if (gateways.isEmpty()) {
            throw new InvalidActivityException(workflow.getId(),
                "Expecting \"if\" keyword to open a new conditional branching, got \"else\"");
          }

          builder = gateways.get(parentId);

          if (builder == null) {
            log.error(
                "This error happens when an activity with \"else\" operation has activity-completed referencing "
                    + "an activity that has another conditional branching, a non existing activity id "
                    + "or no activity-completed is provided");
            throw new InvalidActivityException(workflow.getId(),
                String.format("Expecting activity %s not to have a parent activity with conditional branching, got %s",
                    activity.getId(), parentId));
          } else {
            builder = builder.moveToLastGateway();
            // this condition is now closed, remove it
            gateways.remove(parentId);
          }
        }

        builder = addTask(builder, activity);

        if (onConditional(activity)) {
          // store it to continue other conditional flows (else if, else)
          if (parentId == null) {
            throw new InvalidActivityException(workflow.getId(), String.format(
                "Expecting activity %s not to have a parent activity with conditional branching, "
                    + "got an unknown activity id",
                activity.getId()));
          } else if (parentId.equals("")) {
            throw new InvalidActivityException(workflow.getId(),
                String.format("Starting activity %s cannot have a conditional branching", activity.getId()));
          }
          gateways.put(parentId, builder);
        }

        connectAdditionalActivityFailedEvents(workflow, builder, activity);

        for (ActivityCompletedEvent activityCompletedEvent : activityCompletedEvents) {
          if (parentActivities.containsKey(activityCompletedEvent.getActivityId())) {
            builder.moveToNode(activityCompletedEvent.getActivityId()).connectTo(activity.getId());
          }
        }
      }

      // connect multiple start or intermediate events (can be done once the activity is created only)
      for (AbstractFlowNodeBuilder<?, ?> event : eventsToConnect) {
        event.connectTo(activity.getId());
      }
      eventsToConnect.clear(); // empty it for next activity

      // store loop flows to build later
      if (activity.getOn() != null && activity.getOn().getOneOf() != null) {
        for (Event event : activity.getOn().getOneOf()) {
          if (event.getActivityCompleted() != null) {
            flowsToCreate.put(event.getActivityCompleted().getActivityId(), Pair.of(activity, event));
          }
        }
      }

      // do we need to create a flow for this activity?
      if (flowsToCreate.containsKey(activity.getId())) {
        Pair<BaseActivity, Event> flowToCreate = flowsToCreate.get(activity.getId());
        if (StringUtils.isNotEmpty(flowToCreate.getRight().getActivityCompleted().getIfCondition())) {
          // conditional flow with a gateway
          String loopId = "loop_" + activity.getId();
          builder = builder.exclusiveGateway().id(loopId);
          builder = builder.condition("if",
              flowToCreate.getRight().getActivityCompleted().getIfCondition());
          builder = builder.connectTo(flowToCreate.getLeft().getId())
              .moveToNode(loopId);
        } else {
          // straight flow to a given activity
          builder.connectTo(flowToCreate.getLeft().getId());
        }
        flowsToCreate.remove(activity.getId());
      }

      lastActivity = activity.getId();
    }

    // finish all subprocesses handling form replies
    subProcessedFinish.forEach(subprocess -> subprocess.endEvent().subProcessDone());

    // closed conditions without an else, adding a default end flow
    for (AbstractFlowNodeBuilder<?, ?> gatewayWithoutElse : gateways.values()) {
      gatewayWithoutElse.moveToLastGateway().endEvent();
    }

    // we have a conditional flow/loop left open, without any default flow, set one
    if (builder instanceof ExclusiveGatewayBuilder) {
      builder = builder.endEvent();
    }

    BpmnModelInstance model = builder.done();
    process.addExtensionElement(VariablesListener.create(model, workflow.getVariables()));
    return model;
  }

  private AbstractFlowNodeBuilder<?, ?> addIntermediateEvents(List<AbstractFlowNodeBuilder<?, ?>> eventsToConnect,
      AbstractFlowNodeBuilder<?, ?> builder, String lastActivity, BaseActivity activity,
      Workflow workflow) {
    if (!isFirstActivity(lastActivity) && !activity.getEvents().isEmpty()) {
      for (Event event : activity.getEvents()) {

        if (event.getTimerFired() != null) {
          builder = createOrMoveEventGateway(builder);
          builder = timerStartEvent(builder.intermediateCatchEvent(), event);
          eventsToConnect.add(builder); // will be connected after the activity is created

        } else {
          Optional<String> signalName = eventToMessage.toSignalName(event, workflow);
          if (signalName.isPresent()) {

            builder = createOrMoveEventGateway(builder);
            IntermediateCatchEventBuilder intermediateCatchEventBuilder =
                builder.intermediateCatchEvent().camundaAsyncBefore().name(signalName.get());

            if (isUniqueReplyForm(activity)) {
              intermediateCatchEventBuilder.message(signalName.get());
            } else {
              intermediateCatchEventBuilder.signal(signalName.get());
            }

            builder = intermediateCatchEventBuilder.name(signalName.get());
            eventsToConnect.add(builder); // will be connected after the activity is created
          }
        }
      }

      if (!eventsToConnect.isEmpty()) {
        // last event is automatically created when activity is added
        eventsToConnect.remove(eventsToConnect.size() - 1);
      }
    }
    return builder;
  }

  private AbstractFlowNodeBuilder<?, ?> createOrMoveEventGateway(AbstractFlowNodeBuilder<?, ?> builder) {
    if (builder instanceof IntermediateCatchEventBuilder) {
      builder = builder.moveToLastGateway();
    } else {
      builder = builder.eventBasedGateway();
    }
    return builder;
  }

  private AbstractFlowNodeBuilder<?, ?> startingEvents(Workflow workflow, ProcessBuilder process,
      List<AbstractFlowNodeBuilder<?, ?>> multipleEvents) {
    List<Event> events = getStartingEvents(workflow);
    AbstractFlowNodeBuilder<?, ?> builder = null;
    if (events.isEmpty()) {
      throw new NoStartingEventException(workflow.getId());
    } else {
      for (Event event : events) {
        if (event.getTimerFired() != null) {
          builder = timerStartEvent(process.startEvent(), event);
          multipleEvents.add(builder);
        } else {
          Optional<String> signalName = eventToMessage.toSignalName(event, workflow);
          if (signalName.isPresent()) {
            builder = process.startEvent()
                .camundaAsyncBefore()
                .signal(signalName.get())
                .name(signalName.get());
            multipleEvents.add(builder);
          }
        }
      }
      if (builder == null || multipleEvents.isEmpty()) {
        throw new NoStartingEventException(workflow.getId());
      }
      // last start event is automatically connected, so we don't need it
      multipleEvents.remove(multipleEvents.size() - 1);
    }

    return builder;
  }

  private AbstractFlowNodeBuilder<?, ?> timerStartEvent(AbstractCatchEventBuilder<?, ?> builder, Event event) {
    if (event.getTimerFired().getRepeat() != null) {
      builder = builder
          .timerWithCycle(event.getTimerFired().getRepeat())
          .name("timerFired_cycle");
    } else if (event.getTimerFired().getAt() != null) {
      builder = builder
          .timerWithDate(event.getTimerFired().getAt())
          .name("timerFired_date");
    }
    return builder;
  }

  private boolean isFirstActivity(String lastActivity) {
    return lastActivity.equals("");
  }

  private List<ActivityCompletedEvent> getActivityCompletedEvents(BaseActivity activity) {
    if (activity.getOn() != null && activity.getOn().getActivityCompleted() != null) {
      return new ArrayList<>(Collections.singletonList(activity.getOn().getActivityCompleted()));

    } else if (activity.getOn() != null && activity.getOn().getOneOf() != null) {
      return activity.getOn().getOneOf().stream()
          .map(Event::getActivityCompleted)
          .filter(Objects::nonNull)
          .collect(Collectors.toList());

    } else {
      return Collections.emptyList();
    }
  }

  private boolean onConditional(BaseActivity activity) {
    return activity.getIfCondition() != null
        || (activity.getOn() != null && activity.getOn().getActivityCompleted() != null
        && activity.getOn().getActivityCompleted().getIfCondition() != null);
  }

  private static boolean onFormRepliedEvent(BaseActivity baseActivity) {
    return baseActivity.getOn() != null && baseActivity.getOn().getFormReplied() != null;
  }

  private static boolean onActivityExpired(BaseActivity activity) {
    return !getActivityToExpire(activity).isEmpty();
  }

  private static List<String> getActivityToExpire(BaseActivity activity) {
    List<String> activities = new ArrayList<>();
    if (activity.getOn() != null && activity.getOn().getActivityExpired() != null) {
      activities.add(activity.getOn().getActivityExpired().getActivityId());
    }

    if (activity.getOn() != null && activity.getOn().getOneOf() != null) {
      activities.addAll(activity.getOn()
          .getOneOf()
          .stream()
          .filter(e -> e.getActivityExpired() != null)
          .map(e -> e.getActivityExpired().getActivityId())
          .collect(Collectors.toList()));
    }

    return activities;
  }

  private static ActivityFailedEvent firstOnActivityFailedEvent(BaseActivity activity) {
    if (activity.getOn() != null) {
      if (activity.getOn().getActivityFailed() != null
          && activity.getOn().getActivityFailed().getActivityId() != null) {
        return activity.getOn().getActivityFailed();
      } else if (activity.getOn().getOneOf() != null) {
        return activity.getOn().getOneOf().stream()
            .filter(e -> e.getActivityFailed() != null)
            .findFirst()
            .map(Event::getActivityFailed)
            .orElse(null);
      }
    }
    return null;
  }

  private static List<ActivityFailedEvent> otherOnActivityFailedEvents(BaseActivity activity) {
    if (activity.getOn() != null && activity.getOn().getOneOf() != null) {
      List<ActivityFailedEvent> failedEvents = activity.getOn().getOneOf().stream()
          .map(Event::getActivityFailed)
          .filter(Objects::nonNull)
          .collect(Collectors.toList());

      if (!failedEvents.isEmpty()) {
        // skip the first one it has already been handled
        failedEvents.remove(0);
      }
      return failedEvents;
    }
    return Collections.emptyList();
  }

  private void connectSourceToTarget(AbstractFlowNodeBuilder<?, ?> builder, String source, String target) {
    AbstractFlowNodeBuilder<?, ?> builderOnNode = builder.moveToNode(source);
    // no check on the builder type as connectTo() method is inherited from parent class
    builderOnNode.connectTo(target);
  }

  private void connectAdditionalActivityFailedEvents(Workflow workflow, AbstractFlowNodeBuilder<?, ?> builder,
      BaseActivity activity) {
    List<ActivityFailedEvent> otherActivityFailedEvents = otherOnActivityFailedEvents(activity);
    for (ActivityFailedEvent failedEvent : otherActivityFailedEvents) {
      AbstractFlowNodeBuilder<?, ?> failedActivity = builder.moveToNode(failedEvent.getActivityId());
      if (failedActivity instanceof AbstractActivityBuilder) {
        ((AbstractActivityBuilder<?, ?>) failedActivity).boundaryEvent()
            .name("error_" + activity.getId())
            .error()
            .connectTo(activity.getId());
      } else {
        throw new InvalidActivityException(workflow.getId(),
            String.format("Could not find activity with id %s referenced by activity-failed event",
                failedEvent.getActivityId()));
      }
    }
  }

  private AbstractFlowNodeBuilder<?, ?> formReply(AbstractFlowNodeBuilder<?, ?> builder, BaseActivity activity,
      Map<String, AbstractFlowNodeBuilder<?, ?>> formReplies,
      Map<String, AbstractFlowNodeBuilder<?, ?>> activityToTimeoutBuilderMap) {

    // Forms have default timeout of 24H if none is set
    String timeout = DEFAULT_FORM_REPLIED_EVENT_TIMEOUT;
    if (activity.getOn() != null && StringUtils.isNotEmpty(activity.getOn().getTimeout())) {
      timeout = activity.getOn().getTimeout();
    }

    if (isUniqueReplyForm(activity)) {
      // this form is expecting a single reply, so we can treat it as a simple flow
      AbstractFlowNodeBuilder<?, ?> builderTimeout = builder.camundaAsyncBefore();
      setTimeout(activity, timeout, builderTimeout, formReplies, activityToTimeoutBuilderMap);

    } else {
      /*
          A form reply is a dedicated sub process doing 2 things:
            - waiting for an expiration time, if the expiration time is reached the entire subprocess ends
              and replies are no longer used
            - waiting for reply with an event sub process that is running for each reply
       */
      SubProcessBuilder subProcess = builder.subProcess();

      if (activity.getOn() != null) {
        AbstractFlowNodeBuilder<?, ?> formExpirationBuilder = subProcess.embeddedSubProcess()
            .startEvent()
            .intermediateCatchEvent().timerWithDuration(timeout);
        formReplies.put(activity.getId(), formExpirationBuilder);

        // we add the form reply event sub process inside the subprocess
        builder = subProcess.embeddedSubProcess().eventSubProcess()
            .startEvent()
            .camundaAsyncBefore()
            .interrupting(false) // run multiple instances of the sub process (i.e. multiple replies)
            .message(WorkflowEventToCamundaEvent.FORM_REPLY_PREFIX + activity.getOn().getFormReplied().getFormId())
            .name("formReply");
      }
    }

    return builder;
  }

  private boolean hasTimeout(BaseActivity activity) {
    if (activity.getOn() != null) {
      return StringUtils.isNotEmpty(activity.getOn().getTimeout());
    }
    return false;
  }

  private AbstractFlowNodeBuilder<?, ?> addTask(AbstractFlowNodeBuilder<?, ?> eventBuilder, BaseActivity activity)
      throws JsonProcessingException {
    // hardcoded so we can rely on Camunda's script task instead of a service task
    if (activity instanceof ExecuteScript) {
      return addScriptTask(eventBuilder, (ExecuteScript) activity);
    } else {
      return addServiceTask(eventBuilder, activity);
    }
  }

  private AbstractFlowNodeBuilder<?, ?> addScriptTask(AbstractFlowNodeBuilder<?, ?> builder,
      ExecuteScript scriptActivity) {
    builder = builder.scriptTask()
        .id(scriptActivity.getId())
        .name(scriptActivity.getId())
        .scriptText(scriptActivity.getScript())
        .scriptFormat(ExecuteScript.SCRIPT_ENGINE)
        .camundaExecutionListenerClass(ExecutionListener.EVENTNAME_START, ScriptTaskAuditListener.class);
    return builder;
  }

  private AbstractFlowNodeBuilder<?, ?> addServiceTask(AbstractFlowNodeBuilder<?, ?> builder,
      BaseActivity activity) throws JsonProcessingException {
    builder = builder.serviceTask()
        .id(activity.getId())
        .name(activity.getId())
        .camundaClass(CamundaExecutor.class)
        .camundaInputParameter(CamundaExecutor.EXECUTOR,
            ActivityRegistry.getActivityExecutors().get(activity.getClass()).getName())
        .camundaInputParameter(CamundaExecutor.ACTIVITY,
            CamundaExecutor.OBJECT_MAPPER.writeValueAsString(activity));
    return builder;
  }

  private void setTimeout(BaseActivity activity, String timeout,
      AbstractFlowNodeBuilder<?, ?> builder,
      Map<String, AbstractFlowNodeBuilder<?, ?>> activityExpirations,
      Map<String, AbstractFlowNodeBuilder<?, ?>> activityToTimeoutBuilderMap) {

    IntermediateCatchEventBuilder builderTimeout =
        builder.moveToLastGateway().intermediateCatchEvent().timerWithDuration(timeout);
    activityToTimeoutBuilderMap.put(activity.getId(), builderTimeout);
    activityExpirations.put(activity.getId(), builderTimeout);
  }

  private boolean isUniqueReplyForm(BaseActivity activity) {
    return activity.getOn() != null && activity.getOn().getFormReplied() != null && Boolean.TRUE.equals(
        activity.getOn().getFormReplied().getUnique());
  }

}
