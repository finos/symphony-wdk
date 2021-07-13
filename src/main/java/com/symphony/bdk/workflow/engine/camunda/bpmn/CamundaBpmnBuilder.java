package com.symphony.bdk.workflow.engine.camunda.bpmn;

import com.symphony.bdk.workflow.engine.camunda.CamundaExecutor;
import com.symphony.bdk.workflow.engine.camunda.listener.VariablesListener;
import com.symphony.bdk.workflow.lang.exception.NoCommandToStartException;
import com.symphony.bdk.workflow.lang.exception.NoStartingEventException;
import com.symphony.bdk.workflow.lang.swadl.Activity;
import com.symphony.bdk.workflow.lang.swadl.Event;
import com.symphony.bdk.workflow.lang.swadl.Workflow;
import com.symphony.bdk.workflow.lang.swadl.activity.BaseActivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.builder.ProcessBuilder;
import org.camunda.bpm.model.bpmn.builder.SubProcessBuilder;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaExecutionListener;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CamundaBpmnBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaBpmnBuilder.class);

  private final RepositoryService repositoryService;

  private final String VARIABLES_NAME = "variables";

  @Autowired
  public CamundaBpmnBuilder(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }

  public BpmnModelInstance addWorkflow(Workflow workflow) {
    BpmnModelInstance instance = workflowToBpmn(workflow);

    try {
      repositoryService.createDeployment()
          .addModelInstance(workflow.getName() + ".bpmn", instance)
          .deploy();
    } finally {
      if (LOGGER.isDebugEnabled()) {
        debugWorkflow(workflow, instance);
      }
    }

    return instance;
  }

  private void debugWorkflow(Workflow workflow, BpmnModelInstance instance) {
    Bpmn.writeModelToFile(new File(workflow.getName() + ".bpmn"), instance);
    LOGGER.debug("BPMN file generated to ./{}.bpmn", workflow.getName());
    try {
      // uses https://github.com/bpmn-io/bpmn-to-image
      Runtime.getRuntime().exec(
          String.format("bpmn-to-image --title %s-%s %s.bpmn:%s.png",
              workflow.getName(), Instant.now(), workflow.getName(), workflow.getName()));
      LOGGER.debug("BPMN, image file generated to ./{}.png", workflow.getName());
    } catch (IOException ioException) {
      LOGGER.warn("Failed to convert BPMN to image, make sure it is installed (npm install -g bpmn-to-image)",
          ioException);
    }
  }

  private Optional<Event> getStartingEvent(Workflow workflow) {
    return workflow.getFirstActivity().flatMap(Activity::getEvent);
  }

  private String getCommandToStart(Workflow workflow) {
    Optional<Event> startingEvent = getStartingEvent(workflow);

    if (startingEvent.isEmpty()) {
      throw new NoStartingEventException();
    }

    Optional<String> commandToStart = startingEvent.get().getCommand();

    if (commandToStart.isPresent()) {
      return commandToStart.get();
    }

    throw new NoCommandToStartException();
  }

  @SneakyThrows
  private BpmnModelInstance workflowToBpmn(Workflow workflow) {
    ProcessBuilder process = Bpmn.createExecutableProcess(workflow.getName() + "-" + UUID.randomUUID());

    String commandToStart = getCommandToStart(workflow);

    AbstractFlowNodeBuilder eventBuilder = process
        .startEvent()
        .message("message_" + commandToStart)
        .name(commandToStart);

    boolean hasSubProcess = false;

    for (Activity activity : workflow.getActivities()) {
      Optional<BaseActivity<?>> maybeActivity = activity.getActivity();
      if (maybeActivity.isPresent()) {
        BaseActivity<?> baseActivity = maybeActivity.get();

        Type executorType =
            ((ParameterizedType) (baseActivity.getClass().getGenericSuperclass())).getActualTypeArguments()[0];

        if (baseActivity.getOn() != null && baseActivity.getOn().getFormReply() != null) {
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

          List<? extends BaseActivity<?>> expirationActivities = collectOnExpirationActivities(workflow, baseActivity);
          for (BaseActivity<?> expirationActivity : expirationActivities) {
            eventBuilder = addServiceTask(eventBuilder, expirationActivity);
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
          eventBuilder = addServiceTask(eventBuilder, baseActivity);
        }
      }
    }

    if (hasSubProcess) { // works for simple cases only
      eventBuilder = eventBuilder.endEvent().subProcessDone();
    }

    return addWorkflowVariablesListener(eventBuilder.done(), process, workflow.getVariables());
  }

  private boolean isFormReply(BaseActivity<?> baseActivity) {
    return baseActivity.getOn() != null && baseActivity.getOn().getFormReplied() != null;
  }

  private List<? extends BaseActivity<?>> collectOnExpirationActivities(Workflow workflow,
      BaseActivity<?> targetActivity) {
    return workflow.getActivities().stream()
        .map(Activity::getActivity)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(a -> a.getOn() != null && a.getOn().getActivityExpired() != null)
        .filter(a -> a.getOn().getActivityExpired().getId().equals(targetActivity.getId()))
        .collect(Collectors.toList());
  }

  private AbstractFlowNodeBuilder addServiceTask(AbstractFlowNodeBuilder eventBuilder, BaseActivity<?> activity)
      throws JsonProcessingException {
    Type executorType =
        ((ParameterizedType) (activity.getClass().getGenericSuperclass())).getActualTypeArguments()[0];
    eventBuilder = eventBuilder.serviceTask()
        .id(activity.getId())
        .name(Objects.toString(activity.getName(), activity.getId()))
        .camundaClass(CamundaExecutor.class)
        .camundaInputParameter(CamundaExecutor.EXECUTOR, executorType.getTypeName())
        .camundaInputParameter(CamundaExecutor.ACTIVITY,
            CamundaExecutor.OBJECT_MAPPER.writeValueAsString(activity));
    return eventBuilder;
  }

  private BpmnModelInstance addWorkflowVariablesListener(BpmnModelInstance instance,
      ProcessBuilder process, List<Map<String, Object>> variables) throws JsonProcessingException {
    if(variables != null && !variables.isEmpty()) {
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
