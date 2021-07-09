package com.symphony.bdk.workflow.engine.camunda.bpmn;

import com.symphony.bdk.workflow.engine.camunda.CamundaExecutor;
import com.symphony.bdk.workflow.lang.exception.NoCommandToStartException;
import com.symphony.bdk.workflow.lang.exception.NoStartingEventException;
import com.symphony.bdk.workflow.lang.swadl.Activity;
import com.symphony.bdk.workflow.lang.swadl.Event;
import com.symphony.bdk.workflow.lang.swadl.Workflow;
import com.symphony.bdk.workflow.lang.swadl.activity.BaseActivity;

import lombok.SneakyThrows;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.builder.ProcessBuilder;
import org.camunda.bpm.model.bpmn.builder.SubProcessBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class CamundaBpmnBuilder {

  private final RepositoryService repositoryService;

  @Autowired
  public CamundaBpmnBuilder(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }

  public BpmnModelInstance addWorkflow(Workflow workflow) {
    BpmnModelInstance instance = workflowToBpmn(workflow);

    repositoryService.createDeployment()
        .addModelInstance(workflow.getName() + ".bpmn", instance)
        .deploy();

    return instance;
  }

  private Optional<Event> getStartingEvent(Workflow workflow) {
    if (workflow.getFirstActivity().isPresent()) {
      Activity firstActivity = workflow.getFirstActivity().get();
      return firstActivity.getEvent();
    }
    return Optional.empty();
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
    AbstractFlowNodeBuilder eventBuilder = process.startEvent().message("message_" + commandToStart);

    for (Activity activity : workflow.getActivities()) {

      Optional<BaseActivity<?>> maybeActivity = activity.getActivity();
      if (maybeActivity.isPresent()) {
        BaseActivity<?> baseActivity = maybeActivity.get();

        Type executorType =
            ((ParameterizedType) (baseActivity.getClass().getGenericSuperclass())).getActualTypeArguments()[0];

        if (baseActivity.getOn().getFormReply() != null) {
          /*
            A form reply is a dedicated sub process doing 2 things:
            - waiting for an expiration time, if the expiration time is reached the entire subprocess ends
              and replies are not longer used
            - waiting for reply with an event sub process that is running for each reply
           */
          SubProcessBuilder subProcess = eventBuilder.subProcess();
          subProcess.embeddedSubProcess()
              .startEvent()
              .intermediateCatchEvent().timerWithDuration("PT5S")
              // TODO add expiration activity there
              .endEvent()
              .subProcessDone();

          // we add the form reply event sub process inside the subprocess
          eventBuilder = subProcess.embeddedSubProcess().eventSubProcess()
              .startEvent()
              .interrupting(false) // run multiple instances of the sub process (i.e multiple replies)
              .message("formReply_" + baseActivity.getOn().getFormReply().getId());
        }

        eventBuilder = eventBuilder.serviceTask()
            .id(baseActivity.getId())
            .name(Objects.toString(baseActivity.getName(), baseActivity.getId()))
            .camundaClass(CamundaExecutor.class)
            .camundaInputParameter(CamundaExecutor.EXECUTOR, executorType.getTypeName())
            .camundaInputParameter(CamundaExecutor.ACTIVITY,
                CamundaExecutor.OBJECT_MAPPER.writeValueAsString(baseActivity));

        if (baseActivity.getOn().getFormReply() != null) {
          // TODO if we have subsequent activities after each reply we will have to change this
          eventBuilder = eventBuilder.endEvent().subProcessDone();
        }
      }
    }

    return eventBuilder.done();
  }

}
