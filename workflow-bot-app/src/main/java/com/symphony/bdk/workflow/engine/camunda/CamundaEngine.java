package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.core.service.message.exception.PresentationMLParserException;
import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.engine.ExecutionParameters;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.engine.camunda.audit.AuditTrailLogger;
import com.symphony.bdk.workflow.engine.camunda.bpmn.CamundaBpmnBuilder;
import com.symphony.bdk.workflow.exception.UnauthorizedException;
import com.symphony.bdk.workflow.swadl.exception.UniqueIdViolationException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;
import com.symphony.bdk.workflow.swadl.v1.event.RequestReceivedEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.xml.ModelValidationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CamundaEngine implements WorkflowEngine<BpmnModelInstance> {

  //@Autowired
  private final RepositoryService repositoryService;

  //@Autowired
  private final CamundaBpmnBuilder bpmnBuilder;

  //@Autowired
  private final WorkflowEventToCamundaEvent events;

//  @Autowired
  private final AuditTrailLogger auditTrailLogger;

  private final RuntimeService runtimeService;

  public CamundaEngine(RepositoryService repositoryService, RuntimeService runtimeService, CamundaBpmnBuilder bpmnBuilder,
      WorkflowEventToCamundaEvent events, AuditTrailLogger auditTrailLogger) {
    this.repositoryService = repositoryService;
    this.runtimeService  = runtimeService;
    this.bpmnBuilder = bpmnBuilder;
    this.events = events;
    this.auditTrailLogger = auditTrailLogger;
    this.auditTrailLogger.setRuntimeService(runtimeService);
  }

  @Override
  public void deploy(Workflow workflow) throws IOException {
    Object instance = parseAndValidate(workflow);
    deploy(workflow, instance);
  }

  @Override
  public void deploy(Workflow workflow, Object bpmnInstance) throws IOException {
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
      throw new UnauthorizedException("Request is not authorised");
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
