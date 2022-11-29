package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.engine.ExecutionParameters;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.engine.camunda.audit.AuditTrailLogger;
import com.symphony.bdk.workflow.engine.camunda.bpmn.CamundaBpmnBuilder;
import com.symphony.bdk.workflow.event.RealTimeEventProcessor;
import com.symphony.bdk.workflow.exception.NotFoundException;
import com.symphony.bdk.workflow.exception.UnauthorizedException;
import com.symphony.bdk.workflow.swadl.exception.UniqueIdViolationException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;
import com.symphony.bdk.workflow.swadl.v1.event.RequestReceivedEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.xml.ModelValidationException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CamundaEngine implements WorkflowEngine<BpmnModelInstance> {

  private final RepositoryService repositoryService;

  private final CamundaBpmnBuilder bpmnBuilder;

  private final Map<String, RealTimeEventProcessor<?>> processorRegistry;

  private final AuditTrailLogger auditTrailLogger;

  public CamundaEngine(RepositoryService repositoryService, CamundaBpmnBuilder bpmnBuilder,
      List<RealTimeEventProcessor<?>> processors, AuditTrailLogger auditTrailLogger) {
    this.repositoryService = repositoryService;
    this.bpmnBuilder = bpmnBuilder;
    processorRegistry =
        processors.stream().collect(Collectors.toMap(p -> p.sourceType().getSimpleName(), Function.identity()));
    this.auditTrailLogger = auditTrailLogger;
  }

  @Override
  public void deploy(Workflow workflow) {
    BpmnModelInstance instance = parseAndValidate(workflow);
    deploy(workflow, instance);
  }

  @Override
  public void deploy(Workflow workflow, BpmnModelInstance bpmnInstance) {
    Deployment deployment = bpmnBuilder.deployWorkflow(workflow, (BpmnModelInstance) bpmnInstance);
    log.info("Deployed workflow {} {}", deployment.getId(), deployment.getName());
    auditTrailLogger.deployed(deployment);
  }

  @Override
  public BpmnModelInstance parseAndValidate(Workflow workflow) {
    checkIdsAreUnique(workflow);
    try {
      return bpmnBuilder.parseWorkflowToBpmn(workflow);
    } catch (JsonProcessingException | ModelValidationException exception) {
      throw new IllegalArgumentException(
          String.format("Workflow parsing process failed, \"%s\" may not be a valid workflow.", workflow.getId()),
          exception);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void execute(String workflowId, ExecutionParameters parameters) {

    // check workflow id
    ProcessDefinition processDefinition = this.repositoryService.createProcessDefinitionQuery()
        .active()
        .list()
        .stream()
        .filter(process -> process.getName().equals(workflowId))
        .findFirst()
        .orElseThrow(() -> new NotFoundException("No workflow found with id " + workflowId));

    // check token
    String workflowToken = repositoryService.getDeploymentResources(processDefinition.getDeploymentId())
        .stream()
        .filter(resource -> resource.getName().equals(CamundaBpmnBuilder.DEPLOYMENT_RESOURCE_TOKEN_KEY))
        .map(resource -> new String(resource.getBytes(), StandardCharsets.UTF_8))
        .findFirst()
        .orElse("");

    if (!workflowToken.isEmpty() && !workflowToken.equals(parameters.getToken())) {
      throw new UnauthorizedException("Request is not authorised");
    }

    // dispatch event
    try {
      RealTimeEvent<RequestReceivedEvent> event = toRealTimeEvent(parameters, processDefinition.getName());
      ((RealTimeEventProcessor<RequestReceivedEvent>) processorRegistry.get(
          event.getSource().getClass().getSimpleName())).process(event);
    } catch (Exception e) {
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
  @SuppressWarnings("unchecked")
  public <T> void onEvent(RealTimeEvent<T> event) {
    try {
      ((RealTimeEventProcessor<T>) processorRegistry.get(event.getSource().getClass().getSimpleName())).process(event);
    } catch (Exception e) {
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
