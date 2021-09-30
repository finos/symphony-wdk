package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.core.service.message.exception.PresentationMLParserException;
import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.engine.ExecutionParameters;
import com.symphony.bdk.workflow.engine.UnauthorizedException;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.engine.camunda.audit.AuditTrailLogger;
import com.symphony.bdk.workflow.engine.camunda.bpmn.CamundaBpmnBuilder;
import com.symphony.bdk.workflow.swadl.exception.UniqueIdViolationException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;
import com.symphony.bdk.workflow.swadl.v1.event.RequestReceivedEvent;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
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
  public void deploy(Workflow workflow) throws IOException {
    checkIdsAreUnique(workflow);
    Deployment deployment = bpmnBuilder.addWorkflow(workflow);
    log.info("Deployed workflow {} {}", deployment.getId(), deployment.getName());
    auditTrailLogger.deployed(deployment);
  }

  @Override
  public void execute(String workflowId, ExecutionParameters parameters) {

    // check workflow id
    ProcessDefinition processDefinition = this.repositoryService.createProcessDefinitionQuery()
        .active()
        .list()
        .stream()
        .filter(process -> process.getName().equals(workflowId))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No workflow found with id " + workflowId));

    // check token
    String workflowToken = repositoryService.getDeploymentResources(processDefinition.getDeploymentId())
        .stream()
        .filter(resource -> resource.getName().equals(CamundaBpmnBuilder.DEPLOYMENT_RESOURCE_TOKEN_KEY))
        .map(resource -> new String(resource.getBytes(), StandardCharsets.UTF_8))
        .findFirst()
        .orElse("");

    if (!workflowToken.isEmpty() && !workflowToken.equals(parameters.getToken())) {
      throw new UnauthorizedException("Request token is not valid");
    }

    // dispatch event
    try {
      events.dispatch(toRealTimeEvent(parameters, processDefinition.getName()));
    } catch (PresentationMLParserException e) {
      log.debug("Failed to parse MessageML, should not happen", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void undeploy(String workflowName) {
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
  public void undeployAll() {
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

  private RealTimeEvent<RequestReceivedEvent> toRealTimeEvent(ExecutionParameters parameters, String workflowId) {
    RequestReceivedEvent requestReceivedEvent = new RequestReceivedEvent();
    requestReceivedEvent.setArguments(parameters.getArguments());
    requestReceivedEvent.setToken(parameters.getToken());
    requestReceivedEvent.setWorkflowId(workflowId);
    return new RealTimeEvent<>(null, requestReceivedEvent);
  }
}
