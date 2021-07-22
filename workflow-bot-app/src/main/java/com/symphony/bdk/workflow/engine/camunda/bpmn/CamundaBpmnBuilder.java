package com.symphony.bdk.workflow.engine.camunda.bpmn;

import com.symphony.bdk.workflow.engine.camunda.CamundaExecutor;
import com.symphony.bdk.workflow.engine.camunda.listener.VariablesListener;
import com.symphony.bdk.workflow.lang.ActivityRegistry;
import com.symphony.bdk.workflow.lang.exception.NoStartingEventException;
import com.symphony.bdk.workflow.lang.swadl.Activity;
import com.symphony.bdk.workflow.lang.swadl.Event;
import com.symphony.bdk.workflow.lang.swadl.Workflow;
import com.symphony.bdk.workflow.lang.swadl.activity.BaseActivity;
import com.symphony.bdk.workflow.lang.swadl.activity.ExecuteScript;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CamundaBpmnBuilder {

  private static final String VARIABLES_NAME = "variables";

  private final RepositoryService repositoryService;

  @Autowired
  public CamundaBpmnBuilder(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
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
        .flatMap(Event::getCommand)
        .orElseThrow(NoStartingEventException::new);
  }

  @SneakyThrows
  private BpmnModelInstance workflowToBpmn(Workflow workflow) {
    ProcessBuilder process = Bpmn.createExecutableProcess(createUniqueProcessId(workflow));

    String commandToStart = getCommandToStart(workflow);

    AbstractFlowNodeBuilder<?, ?> eventBuilder = process
        .startEvent()
        .message("message_" + commandToStart)
        .name(commandToStart);

    boolean hasSubProcess = false;

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
            .message("formReply_" + baseActivity.getOn().getFormReply().getId())
            .name("formReply");

        hasSubProcess = true;
      }

      if (baseActivity.getOn() == null || baseActivity.getOn().getActivityExpired() == null) {
        eventBuilder = addTask(eventBuilder, baseActivity);
      }
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
    return baseActivity.getOn() != null && baseActivity.getOn().getFormReply() != null;
  }

  private List<BaseActivity> collectOnExpirationActivities(Workflow workflow,
      BaseActivity targetActivity) {
    return workflow.getActivities().stream()
        .map(Activity::getActivity)
        .filter(a -> a.getOn() != null && a.getOn().getActivityExpired() != null)
        .filter(a -> a.getOn().getActivityExpired().getId().equals(targetActivity.getId()))
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
    eventBuilder.scriptTask()
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
      ProcessBuilder process, List<Map<String, Object>> variables) throws JsonProcessingException {
    if (variables != null) {
      CamundaExecutionListener listener = instance.newInstance(CamundaExecutionListener.class);
      listener.setCamundaEvent(ExecutionListener.EVENTNAME_START);
      listener.setCamundaClass(VariablesListener.class.getName());
      CamundaField field = instance.newInstance(CamundaField.class);
      field.setCamundaName(VARIABLES_NAME);
      field.setCamundaStringValue(variablesAsJsonString(variables));
      listener.getCamundaFields().add(field);

      process.addExtensionElement(listener);
    }
    return instance;
  }

  private String variablesAsJsonString(List<Map<String, Object>> variables) throws JsonProcessingException {
    ObjectNode variablesNode = CamundaExecutor.OBJECT_MAPPER.createObjectNode();

    for (Map<String, Object> variableMap : variables) {
      for (Map.Entry<String, Object> entry : variableMap.entrySet()) {
        variablesNode.put(entry.getKey(), entry.getValue().toString());
      }
    }

    return CamundaExecutor.OBJECT_MAPPER.writeValueAsString(variablesNode);
  }
}
