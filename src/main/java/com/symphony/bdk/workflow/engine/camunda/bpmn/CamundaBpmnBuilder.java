package com.symphony.bdk.workflow.engine.camunda.bpmn;

import com.symphony.bdk.workflow.engine.camunda.CamundaExecutor;
import com.symphony.bdk.workflow.engine.executor.CreateRoomExecutor;
import com.symphony.bdk.workflow.engine.executor.SendMessageExecutor;
import com.symphony.bdk.workflow.lang.exception.NoCommandToStartException;
import com.symphony.bdk.workflow.lang.exception.NoStartingEventException;
import com.symphony.bdk.workflow.lang.swadl.Activity;
import com.symphony.bdk.workflow.lang.swadl.Event;
import com.symphony.bdk.workflow.lang.swadl.Workflow;
import com.symphony.bdk.workflow.util.InputParameterUtils;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.builder.ProcessBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CamundaBpmnBuilder {

  private final RepositoryService repositoryService;

  // run a single workflow at anytime
  private Deployment deploy;

  @Autowired
  public CamundaBpmnBuilder(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }

  public BpmnModelInstance addWorkflow(Workflow workflow) {
    BpmnModelInstance instance = workflowToBpmn(workflow);

    if (deploy != null) {
      repositoryService.deleteDeployment(deploy.getId());
    }
    deploy = repositoryService.createDeployment()
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

  private BpmnModelInstance workflowToBpmn(Workflow workflow) {
    ProcessBuilder process = Bpmn.createExecutableProcess(workflow.getName());

    String commandToStart = getCommandToStart(workflow);
    AbstractFlowNodeBuilder eventBuilder = process.startEvent().message("message_" + commandToStart);

    for (Activity activity : workflow.getActivities()) {
      if (activity.getCreateRoom() != null) {
        eventBuilder = eventBuilder.serviceTask()
            .camundaClass(CamundaExecutor.class)
            .name(activity.getCreateRoom().getName())
            .camundaInputParameter(CamundaExecutor.IMPL, CreateRoomExecutor.class.getName())
            .camundaInputParameter("name", activity.getCreateRoom().getName())
            .camundaInputParameter("public", Boolean.toString(activity.getCreateRoom().isPublic()))
            .camundaInputParameter("description", activity.getCreateRoom().getRoomDescription())
            .camundaInputParameter("uids", InputParameterUtils.longListToString(activity.getCreateRoom().getUids()));
      } else if (activity.getSendMessage() != null) {
        eventBuilder = eventBuilder.serviceTask()
            .camundaClass(CamundaExecutor.class)
            .name(activity.getSendMessage().getName())
            .camundaInputParameter(CamundaExecutor.IMPL, SendMessageExecutor.class.getName());
      }
    }

    return eventBuilder.endEvent().done();
  }

}
