package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.core.service.message.exception.PresentationMLParserException;
import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.engine.camunda.bpmn.CamundaBpmnBuilder;
import com.symphony.bdk.workflow.swadl.exception.UniqueIdViolationException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CamundaEngine implements WorkflowEngine {

  @Autowired
  private RepositoryService repositoryService;

  @Autowired
  private CamundaBpmnBuilder bpmnBuilder;

  @Autowired
  private WorkflowEventToCamundaEvent events;

  @Autowired
  private AuditTrailLogger auditTrailLogger;

  @Override
  public void execute(Workflow workflow) throws IOException {
    checkIdsAreUnique(workflow);
    if (workflow.getId() == null) {
      workflow.setId("workflow_" + UUID.randomUUID());
    }
    Deployment deployment = bpmnBuilder.addWorkflow(workflow);
    log.info("Deployed workflow {}", deployment.getId());
    auditTrailLogger.deployed(deployment);
  }

  @Override
  public void stop(String workflowName) {
    for (Deployment deployment : repositoryService.createDeploymentQuery().deploymentName(workflowName).list()) {
      stop(deployment);
    }
  }

  private void stop(Deployment deployment) {
    repositoryService.deleteDeployment(deployment.getId(), true);
    log.info("Removed workflow {}", deployment.getName());
    auditTrailLogger.undeployed(deployment);
  }

  @Override
  public void stopAll() {
    for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      CamundaEngine.this.stop(deployment);
    }
  }

  @Override
  public <T> void onEvent(RealTimeEvent<T> event) {
    try {
      events.dispatch(event);
    } catch (PresentationMLParserException e) {
      log.error("This error happens when the incoming event has an invalid PresentationML message", e);
    }
  }

  private void checkIdsAreUnique(Workflow workflow) {
    List<String> duplicatedIds = workflow.getActivities()
        .stream()
        .map(activity -> activity.getActivity().getId())
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
        .entrySet()
        .stream()
        .filter(entry -> entry.getValue() > 1)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());

    if (!duplicatedIds.isEmpty()) {
      throw new UniqueIdViolationException(workflow.getId(), duplicatedIds);
    }
  }
}
