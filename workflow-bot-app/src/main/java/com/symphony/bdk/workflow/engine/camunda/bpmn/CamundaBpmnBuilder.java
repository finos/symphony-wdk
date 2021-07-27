package com.symphony.bdk.workflow.engine.camunda.bpmn;

import com.symphony.bdk.workflow.engine.camunda.CamundaExecutor;
import com.symphony.bdk.workflow.engine.camunda.EventToMessage;
import com.symphony.bdk.workflow.engine.camunda.listener.VariablesListener;
import com.symphony.bdk.workflow.lang.ActivityRegistry;
import com.symphony.bdk.workflow.lang.exception.NoStartingEventException;
import com.symphony.bdk.workflow.lang.swadl.Activity;
import com.symphony.bdk.workflow.lang.swadl.Workflow;
import com.symphony.bdk.workflow.lang.swadl.activity.BaseActivity;
import com.symphony.bdk.workflow.lang.swadl.activity.ExecuteScript;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.builder.ProcessBuilder;
import org.camunda.bpm.model.bpmn.builder.SubProcessBuilder;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaExecutionListener;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CamundaBpmnBuilder {

  private final RepositoryService repositoryService;
  private final EventToMessage eventToMessage;

  @Autowired
  public CamundaBpmnBuilder(RepositoryService repositoryService,
      EventToMessage eventToMessage) {
    this.repositoryService = repositoryService;
    this.eventToMessage = eventToMessage;
  }

  public BpmnModelInstance addWorkflow(Workflow workflow) {
    BpmnModelInstance instance = workflowToBpmn(workflow);

    try {
      repositoryService.createDeployment()
          .name(workflow.getName())
          .addModelInstance(workflow.getName() + ".bpmn", instance)
          .deploy();
    } finally {
      if (log.isDebugEnabled()) {
        debugWorkflow(workflow, instance);
      }
    }

    return instance;
  }

  private void debugWorkflow(Workflow workflow, BpmnModelInstance instance) {
    // avoid polluting current folder in dev, keep it working for deployment/Docker
    File outputFolder = new File("./build");
    if (!outputFolder.exists() || !outputFolder.isDirectory()) {
      outputFolder = new File(".");
    }

    File bpmnFile = new File(outputFolder, workflow.getName() + ".bpmn");
    Bpmn.writeModelToFile(bpmnFile, instance);
    log.debug("BPMN file generated to {}", bpmnFile);
    try {
      // uses https://github.com/bpmn-io/bpmn-to-image
      File pngFile = new File(outputFolder, workflow.getName() + ".png");
      Runtime.getRuntime().exec(
          String.format("bpmn-to-image --title %s-%s %s:%s",
              workflow.getName(), Instant.now(), bpmnFile, pngFile));
      log.debug("BPMN, image outputFolder generated to {}", pngFile);
    } catch (IOException ioException) {
      log.warn("Failed to convert BPMN to image, make sure it is installed (npm install -g bpmn-to-image)",
          ioException);
    }
  }

  private String getCommandToStart(Workflow workflow) {
    return workflow.getFirstActivity()
        .flatMap(Activity::getEvent)
        .flatMap(eventToMessage::toMessageName)
        .orElseThrow(NoStartingEventException::new);
  }

  @SneakyThrows
  private BpmnModelInstance workflowToBpmn(Workflow workflow) {
    ProcessBuilder process = Bpmn.createExecutableProcess(createUniqueProcessId(workflow));

    String commandToStart = getCommandToStart(workflow);

    AbstractFlowNodeBuilder<?, ?> eventBuilder = process
        .startEvent()
        .message(commandToStart)
        .name(commandToStart);

    boolean hasSubProcess = false;
    // TODO do we need a first pass to add all the implicit events
    // then build up an execution graph
    // and navigate it in order to facilitate the fluent api usage
    // TODO what if I use activity finished to by pass sequential order?
    // i.e declare tasks in the wrong order?
    boolean newGateway = false; // should be a stack?
    String lastActivity = "";
    Map<String, AbstractFlowNodeBuilder<?, ?>> gateways = new HashMap<>();
    Map<String, String> gatewayLasts = new HashMap<>();
    for (Activity activity : workflow.getActivities()) {
      BaseActivity baseActivity = activity.getActivity();

      if (isFormReply(baseActivity)) {
        /*
          A form reply is a dedicated sub process doing 2 things:
          - waiting for an expiration time, if the expiration time is reached the entire subprocess ends
            and replies are no longer used
          - waiting for reply with an event sub process that is running for each reply
         */
        SubProcessBuilder subProcess = eventBuilder.subProcess();
        eventBuilder = subProcess.embeddedSubProcess()
            .startEvent()
            .intermediateCatchEvent().timerWithDuration(baseActivity.getOn().getTimeout());

        List<? extends BaseActivity> expirationActivities = collectOnExpirationActivities(workflow, baseActivity);
        for (BaseActivity expirationActivity : expirationActivities) {
          eventBuilder = addTask(eventBuilder, expirationActivity);
        }

        eventBuilder.endEvent()
            .subProcessDone();

        // we add the form reply event sub process inside the subprocess
        eventBuilder = subProcess.embeddedSubProcess().eventSubProcess()
            .startEvent()
            .interrupting(false) // run multiple instances of the sub process (i.e multiple replies)
            .message("formReply_" + baseActivity.getOn().getFormReplied().getId())
            .name("formReply");

        hasSubProcess = true;
      }

      if (baseActivity.getOn() == null || baseActivity.getOn().getActivityExpired() == null) {

        if (baseActivity.getIfCondition() != null) {
          // has an on/activity -> find the node to start with
          if (baseActivity.getOn() != null && baseActivity.getOn().getActivityFinished() != null) {
            eventBuilder = gateways.get(baseActivity.getOn().getActivityFinished().getActivityId());
            eventBuilder = eventBuilder.moveToLastGateway()
                .condition("if", baseActivity.getIfCondition());
            newGateway = false;
          } else {
            // a new branch
            eventBuilder = eventBuilder.exclusiveGateway()
                .condition("if", baseActivity.getIfCondition());
            newGateway = true;
            gatewayLasts.put(baseActivity.getId(), lastActivity); // TODO hard coded case, not generic
          }
        }

        if (baseActivity.getElseCondition() != null) {
          if (baseActivity.getOn() != null && baseActivity.getOn().getActivityFinished() != null) {
            eventBuilder = gateways.get(baseActivity.getOn().getActivityFinished().getActivityId());
            eventBuilder = eventBuilder.moveToLastGateway();
            gateways.remove(baseActivity.getOn().getActivityFinished().getActivityId());
          } else {
            eventBuilder = eventBuilder.moveToLastGateway();
            gateways.remove(gatewayLasts.get(lastActivity));
            // we need to remove a gateway here too
            newGateway = false;
          }
        }

        eventBuilder = addTask(eventBuilder, baseActivity);

        if (baseActivity.getIfCondition() != null && newGateway) {
          gateways.put(lastActivity, eventBuilder);
        }
        lastActivity = baseActivity.getId();
      }
    }

    for (AbstractFlowNodeBuilder<?, ?> end : gateways.values()) {
      end.moveToLastGateway().endEvent();
    }

    if (hasSubProcess) { // works for simple cases only
      eventBuilder = eventBuilder.endEvent().subProcessDone();
    }

    return addWorkflowVariablesListener(eventBuilder.done(), process, workflow.getVariables());
  }

  private String createUniqueProcessId(Workflow workflow) {
    // spaces are not supported in BPMN here
    return workflow.getName().replaceAll("\\s+", "_") + "-" + UUID.randomUUID();
  }

  private boolean isFormReply(BaseActivity baseActivity) {
    return baseActivity.getOn() != null && baseActivity.getOn().getFormReplied() != null;
  }

  private List<BaseActivity> collectOnExpirationActivities(Workflow workflow,
      BaseActivity targetActivity) {
    return workflow.getActivities().stream()
        .map(Activity::getActivity)
        .filter(a -> a.getOn() != null && a.getOn().getActivityExpired() != null)
        .filter(a -> a.getOn().getActivityExpired().getActivityId().equals(targetActivity.getId()))
        .collect(Collectors.toList());
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

  private AbstractFlowNodeBuilder<?, ?> addScriptTask(AbstractFlowNodeBuilder<?, ?> eventBuilder,
      ExecuteScript scriptActivity) {
    eventBuilder = eventBuilder.scriptTask()
        .id(scriptActivity.getId())
        .name(Objects.toString(scriptActivity.getName(), scriptActivity.getId()))
        .scriptText(scriptActivity.getScript())
        .scriptFormat(ExecuteScript.SCRIPT_ENGINE);
    return eventBuilder;
  }

  private AbstractFlowNodeBuilder<?, ?> addServiceTask(AbstractFlowNodeBuilder<?, ?> eventBuilder,
      BaseActivity activity) throws JsonProcessingException {
    eventBuilder = eventBuilder.serviceTask()
        .id(activity.getId())
        .name(Objects.toString(activity.getName(), activity.getId()))
        .camundaClass(CamundaExecutor.class)
        .camundaInputParameter(CamundaExecutor.EXECUTOR,
            ActivityRegistry.getActivityExecutors().get(activity.getClass()).getName())
        .camundaInputParameter(CamundaExecutor.ACTIVITY,
            CamundaExecutor.OBJECT_MAPPER.writeValueAsString(activity));
    return eventBuilder;
  }

  private BpmnModelInstance addWorkflowVariablesListener(BpmnModelInstance instance,
      ProcessBuilder process, Map<String, Object> variables) throws JsonProcessingException {
    if (variables != null) {
      CamundaExecutionListener listener = instance.newInstance(CamundaExecutionListener.class);
      listener.setCamundaEvent(ExecutionListener.EVENTNAME_START);
      listener.setCamundaClass(VariablesListener.class.getName());
      CamundaField field = instance.newInstance(CamundaField.class);
      field.setCamundaName(VariablesListener.VARIABLES_FIELD);
      field.setCamundaStringValue(variablesAsJsonString(variables));
      listener.getCamundaFields().add(field);

      process.addExtensionElement(listener);
    }
    return instance;
  }

  private String variablesAsJsonString(Map<String, Object> variables) throws JsonProcessingException {
    return CamundaExecutor.OBJECT_MAPPER.writeValueAsString(variables);
  }
}
