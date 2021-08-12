package com.symphony.bdk.workflow.engine.camunda.bpmn;

import com.symphony.bdk.workflow.engine.camunda.CamundaExecutor;
import com.symphony.bdk.workflow.engine.camunda.WorkflowEventToCamundaEvent;
import com.symphony.bdk.workflow.engine.camunda.listener.VariablesListener;
import com.symphony.bdk.workflow.swadl.ActivityRegistry;
import com.symphony.bdk.workflow.swadl.exception.NoStartingEventException;
import com.symphony.bdk.workflow.swadl.v1.Activity;
import com.symphony.bdk.workflow.swadl.v1.Event;
import com.symphony.bdk.workflow.swadl.v1.Workflow;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;
import com.symphony.bdk.workflow.swadl.v1.activity.ExecuteScript;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.AbstractCatchEventBuilder;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.builder.ExclusiveGatewayBuilder;
import org.camunda.bpm.model.bpmn.builder.IntermediateCatchEventBuilder;
import org.camunda.bpm.model.bpmn.builder.ProcessBuilder;
import org.camunda.bpm.model.bpmn.builder.SubProcessBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Events are created with async before to make sure they are not blocking the dispatch of events (starting or
 * intermediate). This way, 2 workflows listening to the same event are started in parallel
 */
@Slf4j
@Component
public class CamundaBpmnBuilder {

  private final RepositoryService repositoryService;
  private final WorkflowEventToCamundaEvent eventToMessage;

  @Autowired
  public CamundaBpmnBuilder(RepositoryService repositoryService,
      WorkflowEventToCamundaEvent eventToMessage) {
    this.repositoryService = repositoryService;
    this.eventToMessage = eventToMessage;
  }

  public Deployment addWorkflow(Workflow workflow) {
    BpmnModelInstance instance = workflowToBpmn(workflow);

    try {
      return repositoryService.createDeployment()
          .name(workflow.getName())
          .addModelInstance(workflow.getName() + ".bpmn", instance)
          .deploy();
    } finally {
      if (log.isDebugEnabled()) {
        WorkflowDebugger.generateDebugFiles(workflow.getName(), instance);
      }
    }
  }

  private List<Event> getStartingEvents(Workflow workflow) {
    return workflow.getFirstActivity()
        .map(Activity::getEvents)
        .orElseThrow(NoStartingEventException::new);
  }

  @SneakyThrows
  private BpmnModelInstance workflowToBpmn(Workflow workflow) {
    ProcessBuilder process = Bpmn.createExecutableProcess(createUniqueProcessId(workflow));

    // a workflow starts with at least one named signal event
    List<AbstractFlowNodeBuilder<?, ?>> eventsToConnect = new ArrayList<>();
    AbstractFlowNodeBuilder<?, ?> builder = startingEvents(workflow, process, eventsToConnect);

    // we have to carry a bit of state while processing the activities
    String lastActivity = "";
    Map<String, String> parentActivities = new HashMap<>();

    Map<String, AbstractFlowNodeBuilder<?, ?>> formExpirations = new HashMap<>();
    Map<String, AbstractFlowNodeBuilder<?, ?>> gateways = new HashMap<>();
    Map<String, Pair<BaseActivity, Event>> flowsToCreate = new HashMap<>();

    // then each activity is processed
    for (Activity activityContainer : workflow.getActivities()) {
      BaseActivity activity = activityContainer.getActivity();

      builder = addIntermediateEvents(eventsToConnect, builder, lastActivity, activity);

      // process events starting the activity
      if (onFormRepliedEvent(activity)) {
        builder = formReply(builder, activity, formExpirations);
      }

      if (onActivityExpired(activity)) {
        // add the activity as a form expiration
        AbstractFlowNodeBuilder<?, ?> formExpirationBuilder =
            formExpirations.get(activity.getOn().getActivityExpired().getActivityId());
        formExpirationBuilder = addTask(formExpirationBuilder, activity);
        // update it for the next activities
        formExpirations.put(activity.getOn().getActivityExpired().getActivityId(), formExpirationBuilder);

      } else {
        // add the activity in the normal flow

        if (activity.getOn() != null && activity.getOn().getActivityCompleted() != null) {
          parentActivities.put(activity.getId(), activity.getOn().getActivityCompleted().getActivityId());
        } else {
          // implicit parent activity is the one declared before
          parentActivities.put(activity.getId(), lastActivity);
        }

        if (onConditional(activity)) {
          if (gateways.containsKey(parentActivities.get(activity.getId()))) {
            // we already opened a gateway, so it is an 'else if'
            builder = gateways.get(parentActivities.get(activity.getId()));
            builder = builder.moveToLastGateway();
          } else {
            // otherwise, it is a new 'if'
            builder = builder.exclusiveGateway();
          }
          builder = builder.condition("if", activity.getIfCondition());
        }

        if (activity.getElseCondition() != null) {
          builder = gateways.get(parentActivities.get(activity.getId()));
          builder = builder.moveToLastGateway();
          // this condition is now closed, remove it
          gateways.remove(parentActivities.get(activity.getId()));
        }

        builder = addTask(builder, activity);

        if (onConditional(activity)) {
          // store it to continue other conditional flows (else if, else)
          gateways.put(parentActivities.get(activity.getId()), builder);
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
        String loopId = "loop_" + activity.getId();
        builder = builder.exclusiveGateway().id(loopId)
            .condition("if",
                flowsToCreate.get(activity.getId()).getRight().getActivityCompleted().getIfCondition())
            .connectTo(flowsToCreate.get(activity.getId()).getLeft().getId())
            .moveToNode(loopId);
        flowsToCreate.remove(activity.getId());
      }

      lastActivity = activity.getId();
    }

    // we may have collected flows that we did not connect yet
    for (Map.Entry<String, Pair<BaseActivity, Event>> flow : flowsToCreate.entrySet()) {
      builder.moveToNode(flow.getKey()).connectTo(flow.getValue().getLeft().getId());
    }

    // finish all subprocesses handling form replies
    for (AbstractFlowNodeBuilder<?, ?> subProcessBuilder : formExpirations.values()) {
      subProcessBuilder.endEvent().subProcessDone();
    }

    // closed conditions without an else, adding a default end flow
    for (AbstractFlowNodeBuilder<?, ?> gatewayWithoutElse : gateways.values()) {
      gatewayWithoutElse.moveToLastGateway().endEvent();
    }

    // we have a flow/loop left open, without any default flow, set one
    if (builder instanceof ExclusiveGatewayBuilder) {
      builder = builder.endEvent();
    }

    BpmnModelInstance model = builder.done();
    process.addExtensionElement(VariablesListener.create(model, workflow.getVariables()));
    return model;
  }

  private AbstractFlowNodeBuilder<?, ?> addIntermediateEvents(List<AbstractFlowNodeBuilder<?, ?>> eventsToConnect,
      AbstractFlowNodeBuilder<?, ?> builder, String lastActivity, BaseActivity activity) {
    if (!isFirstActivity(lastActivity) && !activity.getEvents().isEmpty()) {
      for (Event event : activity.getEvents()) {

        if (event.getTimerFired() != null) {
          builder = createOrMoveEventGateway(builder);
          builder = timerStartEvent(builder.intermediateCatchEvent(), event);
          eventsToConnect.add(builder); // will be connected after the activity is created

        } else {
          Optional<String> signalName = eventToMessage.toSignalName(event);
          if (signalName.isPresent()) {
            builder = createOrMoveEventGateway(builder);
            builder = builder.intermediateCatchEvent()
                .camundaAsyncBefore()
                .signal(signalName.get())
                .name(signalName.get());
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
      throw new NoStartingEventException();
    } else {
      for (Event event : events) {
        if (event.getTimerFired() != null) {
          builder = timerStartEvent(process.startEvent(), event);
          multipleEvents.add(builder);
        } else {
          Optional<String> signalName = eventToMessage.toSignalName(event);
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
        throw new NoStartingEventException();
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

  private boolean onConditional(BaseActivity activity) {
    return activity.getIfCondition() != null
        || (activity.getOn() != null && activity.getOn().getActivityCompleted() != null
        && activity.getOn().getActivityCompleted().getIfCondition() != null);
  }

  private static String createUniqueProcessId(Workflow workflow) {
    // spaces are not supported in BPMN here
    return workflow.getName().replaceAll("\\s+", "_") + "-" + UUID.randomUUID();
  }

  private static boolean onFormRepliedEvent(BaseActivity baseActivity) {
    return baseActivity.getOn() != null && baseActivity.getOn().getFormReplied() != null;
  }

  private static boolean onActivityExpired(BaseActivity activity) {
    return activity.getOn() != null && activity.getOn().getActivityExpired() != null;
  }

  /*
    A form reply is a dedicated sub process doing 2 things:
      - waiting for an expiration time, if the expiration time is reached the entire subprocess ends
        and replies are no longer used
      - waiting for reply with an event sub process that is running for each reply
   */
  private AbstractFlowNodeBuilder<?, ?> formReply(AbstractFlowNodeBuilder<?, ?> builder, BaseActivity activity,
      Map<String, AbstractFlowNodeBuilder<?, ?>> formReplies) {
    SubProcessBuilder subProcess = builder.subProcess();

    AbstractFlowNodeBuilder<?, ?> formExpirationBuilder = subProcess.embeddedSubProcess()
        .startEvent()
        .intermediateCatchEvent().timerWithDuration(activity.getOn().getTimeout());
    formReplies.put(activity.getId(), formExpirationBuilder);

    // we add the form reply event sub process inside the subprocess
    builder = subProcess.embeddedSubProcess().eventSubProcess()
        .startEvent()
        .camundaAsyncBefore()
        .interrupting(false) // run multiple instances of the sub process (i.e multiple replies)
        .message(WorkflowEventToCamundaEvent.FORM_REPLY_PREFIX + activity.getOn().getFormReplied().getId())
        .name("formReply");
    return builder;
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
        .name(Objects.toString(scriptActivity.getName(), scriptActivity.getId()))
        .scriptText(scriptActivity.getScript())
        .scriptFormat(ExecuteScript.SCRIPT_ENGINE);
    return builder;
  }

  private AbstractFlowNodeBuilder<?, ?> addServiceTask(AbstractFlowNodeBuilder<?, ?> builder,
      BaseActivity activity) throws JsonProcessingException {
    builder = builder.serviceTask()
        .id(activity.getId())
        .name(Objects.toString(activity.getName(), activity.getId()))
        .camundaClass(CamundaExecutor.class)
        .camundaInputParameter(CamundaExecutor.EXECUTOR,
            ActivityRegistry.getActivityExecutors().get(activity.getClass()).getName())
        .camundaInputParameter(CamundaExecutor.ACTIVITY,
            CamundaExecutor.OBJECT_MAPPER.writeValueAsString(activity));
    return builder;
  }

}
