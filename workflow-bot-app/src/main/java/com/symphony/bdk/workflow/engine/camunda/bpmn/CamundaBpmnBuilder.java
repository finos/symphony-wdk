package com.symphony.bdk.workflow.engine.camunda.bpmn;

import static com.symphony.bdk.workflow.engine.camunda.bpmn.BpmnBuilderHelper.hasActivitiesOnly;
import static com.symphony.bdk.workflow.engine.camunda.bpmn.BpmnBuilderHelper.hasAllConditionalChildren;
import static com.symphony.bdk.workflow.engine.camunda.bpmn.BpmnBuilderHelper.hasConditionalString;
import static com.symphony.bdk.workflow.engine.camunda.bpmn.BpmnBuilderHelper.hasLoopAfterSubProcess;

import com.symphony.bdk.workflow.engine.WorkflowDirectGraph;
import com.symphony.bdk.workflow.engine.WorkflowDirectGraph.NodeChildren;
import com.symphony.bdk.workflow.engine.WorkflowDirectGraphBuilder;
import com.symphony.bdk.workflow.engine.WorkflowNode;
import com.symphony.bdk.workflow.engine.WorkflowNodeType;
import com.symphony.bdk.workflow.engine.camunda.WorkflowEventToCamundaEvent;
import com.symphony.bdk.workflow.engine.camunda.bpmn.builder.WorkflowNodeBpmnBuilderFactory;
import com.symphony.bdk.workflow.engine.camunda.variable.VariablesListener;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.DeploymentBuilder;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.builder.ExclusiveGatewayBuilder;
import org.camunda.bpm.model.bpmn.builder.ProcessBuilder;
import org.camunda.bpm.model.bpmn.builder.SubProcessBuilder;
import org.camunda.bpm.model.xml.ModelValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Events are created with async before to make sure they are not blocking the dispatch of events (starting or
 * intermediate). This way, 2 workflows listening to the same event are started in parallel
 */
@Slf4j
@Component
public class CamundaBpmnBuilder {
  public static final String DEPLOYMENT_RESOURCE_TOKEN_KEY = "WORKFLOW_TOKEN";
  public static final String EXCLUSIVE_GATEWAY_SUFFIX = "_exclusive_gateway";
  public static final String EVENT_GATEWAY_SUFFIX = "_event_gateway";
  public static final String FORK_GATEWAY = "_fork_gateway";

  private final RepositoryService repositoryService;
  private final WorkflowEventToCamundaEvent eventToMessage;
  private final WorkflowNodeBpmnBuilderFactory builderFactory;

  @Autowired
  public CamundaBpmnBuilder(RepositoryService repositoryService, WorkflowEventToCamundaEvent eventToMessage,
      WorkflowNodeBpmnBuilderFactory builderFactory) {
    this.repositoryService = repositoryService;
    this.eventToMessage = eventToMessage;
    this.builderFactory = builderFactory;
  }

  public BpmnModelInstance parseWorkflowToBpmn(Workflow workflow)
      throws JsonProcessingException, ModelValidationException {
    BpmnModelInstance instance = workflowToBpmn(workflow);
    try {
      Bpmn.validateModel(instance);
      log.debug("workflow [{}] has been successfully validated.", workflow.getId());
      return instance;
    } finally {
      log.debug("workflow [{}]'s validation is done.", workflow.getId());
      if (log.isDebugEnabled()) {
        WorkflowDebugger.generateDebugFiles(workflow.getId(), instance);
      }
    }
  }

  public Deployment deployWorkflow(Workflow workflow, BpmnModelInstance instance) {
    DeploymentBuilder deploymentBuilder = repositoryService.createDeployment()
        .name(workflow.getId())
        .addModelInstance(workflow.getId() + ".bpmn", instance);
    return setWorkflowTokenIfExists(deploymentBuilder, workflow).deploy();
  }

  private DeploymentBuilder setWorkflowTokenIfExists(DeploymentBuilder deploymentBuilder, Workflow workflow) {
    workflow.getActivities().forEach(activity -> {
      Optional<String> token = activity.getEvents().getEvents()
          .stream()
          .filter(event -> event.getRequestReceived() != null && event.getRequestReceived().getToken() != null)
          .map(event -> event.getRequestReceived().getToken())
          .findFirst();

      token.ifPresent(s -> deploymentBuilder.addString(DEPLOYMENT_RESOURCE_TOKEN_KEY, s));
    });

    return deploymentBuilder;
  }

  private BpmnModelInstance workflowToBpmn(Workflow workflow) throws JsonProcessingException {
    // spaces are not supported in BPMN here
    String processId = workflow.getId().replaceAll("\\s+", "");
    ProcessBuilder process = Bpmn.createExecutableProcess(processId).name(workflow.getId());

    WorkflowDirectGraph workflowDirectGraph = new WorkflowDirectGraphBuilder(workflow, eventToMessage).build();
    BuildProcessContext context = new BuildProcessContext(workflowDirectGraph, process);
    buildWorkflowInDfs(new NodeChildren(context.getStartEvents()), "", context);
    AbstractFlowNodeBuilder<?, ?> builder = closeUpSubProcessesIfAny(context, context.getLastNodeBuilder());
    BpmnModelInstance model = builder.done();
    process.addExtensionElement(VariablesListener.create(model, workflow.getVariables()));
    return model;
  }

  private AbstractFlowNodeBuilder<?, ?> closeUpSubProcessesIfAny(BuildProcessContext context,
      AbstractFlowNodeBuilder<?, ?> builder) {
    while (context.hasEventSubProcess()) {
      builder = context.removeLastEventSubProcessBuilder().subProcessDone();
      context.cacheSubProcessTimeoutToDone((SubProcessBuilder) builder);
      builder = builder.endEvent();
    }
    while (context.hasTimeoutSubProcess()) {
      context.removeLastSubProcessTimeoutBuilder().endEvent();
    }
    return builder;
  }

  private void buildWorkflowInDfs(NodeChildren nodes, String parentNodeId, BuildProcessContext context)
      throws JsonProcessingException {
    for (String currentNodeId : nodes.getChildren()) {
      log.trace("build node [{}] from parent node [{}]", currentNodeId, parentNodeId);
      WorkflowNode currentNode = context.readWorkflowNode(currentNodeId);
      boolean alreadyBuilt = context.isAlreadyBuilt(currentNodeId);
      log.trace("is a node already built ? [{}]", alreadyBuilt);
      AbstractFlowNodeBuilder<?, ?> builder = context.getNodeBuilder(parentNodeId);
      builder = builderFactory.getBuilder(currentNode).connect(currentNode, parentNodeId, builder, context);
      if (!alreadyBuilt) {
        log.trace("compute node [{}] children nodes", currentNodeId);
        computeChildren(currentNode, builder, context);
      }
    }
  }

  private void computeChildren(WorkflowNode currentNode, AbstractFlowNodeBuilder<?, ?> builder,
      BuildProcessContext context) throws JsonProcessingException {
    String currentNodeId = currentNode.getId();
    NodeChildren currentNodeChildren = context.readChildren(currentNodeId);
    if (currentNodeChildren != null) {
      if (currentNodeChildren.getGateway() == WorkflowDirectGraph.Gateway.PARALLEL) {
        builder = builder.parallelGateway(currentNodeId + FORK_GATEWAY);
      } else {
        builder =
            exclusiveSubTreeNodes(currentNodeId, currentNode.getElementType(), builder, context, currentNodeChildren);
      }
      context.addNodeBuilder(currentNodeId, builder); // cache the builder to reuse for its kids
      buildWorkflowInDfs(currentNodeChildren, currentNodeId, context);
      if (hasAllConditionalChildren(context, currentNodeChildren, currentNodeId)
          && builder instanceof ExclusiveGatewayBuilder) {
        log.trace("after recursive, add default end event to the gateway");
        // add a default endEvent to the gateway
        builder.endEvent();
      }
    } else {
      log.trace("the node [{}] is a leaf node", currentNodeId);
      leafNode(currentNodeId, builder, context);
    }
  }

  private AbstractFlowNodeBuilder<?, ?> exclusiveSubTreeNodes(String currentNodeId, WorkflowNodeType currentNodeType,
      AbstractFlowNodeBuilder<?, ?> builder, BuildProcessContext context, NodeChildren currentNodeChildren) {
    if (currentNodeType == WorkflowNodeType.FORM_REPLIED_EVENT || hasFormRepliedEvent(context,
        currentNodeChildren)) {
      log.trace("the node [{}] itself or one of its children is a form replied event", currentNodeId);
      return builder;
    }
    // in case of conditional loop, add a default end event
    if (BpmnBuilderHelper.isConditionalLoop(builder, context, currentNodeChildren)) {
      log.trace("there is a conditional loop from the node [{}], add default end event to the gateway loop",
          currentNodeId);
      builder.endEvent();
      return builder;
    }
    if (hasLoopAfterSubProcess(context, currentNodeChildren)) {
      builder = BpmnBuilderHelper.endEventSubProcess(context, builder);
    }
    boolean activities = hasActivitiesOnly(context, currentNodeChildren);
    // either child or current node is conditional, since the condition can be defined at parent event or activity itself
    boolean conditional = hasConditionalString(context, currentNodeChildren, currentNodeId);
    log.trace("are the children of the node [{}]'s all activities ? [{}], is there any condition in children ? [{}]",
        currentNodeId, activities, conditional);
    builder = addGateway(currentNodeId, builder, activities, conditional);
    return builder;
  }

  private AbstractFlowNodeBuilder<?, ?> addGateway(String currentNodeId, AbstractFlowNodeBuilder<?, ?> builder,
      boolean activities, boolean conditional) {
    // determine the gateway type
    if (activities && conditional) {
      log.trace("an exclusive gateway is added follow the node [{}]", currentNodeId);
      builder = builder.exclusiveGateway(currentNodeId + EXCLUSIVE_GATEWAY_SUFFIX);
    } else if (!activities) {
      log.trace("an event gateway is added follow the node [{}]", currentNodeId);
      builder = builder.eventBasedGateway().id(currentNodeId + EVENT_GATEWAY_SUFFIX);
    }
    return builder;
  }

  private void leafNode(String currentNodeId, AbstractFlowNodeBuilder<?, ?> camundaBuilder,
      BuildProcessContext context) {
    camundaBuilder = camundaBuilder.endEvent();
    context.addNodeBuilder(currentNodeId, camundaBuilder);
    context.addLastNodeBuilder(camundaBuilder);
  }

  private boolean hasFormRepliedEvent(BuildProcessContext context, NodeChildren currentNodeChildren) {
    return currentNodeChildren.getChildren()
        .stream()
        .anyMatch(s -> context.readWorkflowNode(s).getElementType() == WorkflowNodeType.FORM_REPLIED_EVENT);
  }
}
