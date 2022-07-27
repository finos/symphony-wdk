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

  private final RepositoryService repositoryService;
  private final WorkflowEventToCamundaEvent eventToMessage;
  private final WorkflowNodeBpmnBuilderFactory builderFactory;

  @Autowired
  public CamundaBpmnBuilder(RepositoryService repositoryService,
      WorkflowEventToCamundaEvent eventToMessage, WorkflowNodeBpmnBuilderFactory builderFactory) {
    this.repositoryService = repositoryService;
    this.eventToMessage = eventToMessage;
    this.builderFactory = builderFactory;
  }

  public Deployment addWorkflow(Workflow workflow) throws JsonProcessingException {
    BpmnModelInstance instance = workflowToBpmn(workflow);
    try {
      DeploymentBuilder deploymentBuilder = repositoryService.createDeployment()
          .name(workflow.getId())
          .addModelInstance(workflow.getId() + ".bpmn", instance);
      return setWorkflowTokenIfExists(deploymentBuilder, workflow).deploy();
    } finally {
      if (log.isDebugEnabled()) {
        WorkflowDebugger.generateDebugFiles(workflow.getId(), instance);
      }
    }
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
      WorkflowNode currentNode = context.readWorkflowNode(currentNodeId);
      AbstractFlowNodeBuilder<?, ?> builder = context.getNodeBuilder(parentNodeId);
      boolean alreadyBuilt = context.isAlreadyBuilt(currentNodeId);
      builder = builderFactory.getBuilder(currentNode).connect(currentNode, parentNodeId, builder, context);
      if (!alreadyBuilt) {
        computeChildren(currentNodeId, currentNode, builder, context);
      }
    }
  }

  private void computeChildren(String currentNodeId, WorkflowNode currentNode,
      AbstractFlowNodeBuilder<?, ?> builder, BuildProcessContext context) throws JsonProcessingException {
    NodeChildren currentNodeChildren = context.readChildren(currentNode.getId());
    if (currentNodeChildren != null) {
      builder = subTreeNodes(currentNodeId, currentNode, builder, context, currentNodeChildren);
      context.addNodeBuilder(currentNodeId, builder); // cache the builder to reuse for its kids
      buildWorkflowInDfs(currentNodeChildren, currentNodeId, context);
      if (hasAllConditionalChildren(context, currentNodeChildren, currentNodeId)
          && builder instanceof ExclusiveGatewayBuilder) {
        // add a default endEvent to the gateway
        builder.endEvent();
      }
    } else {
      leafNode(currentNodeId, builder, context);
    }
  }

  private AbstractFlowNodeBuilder<?, ?> subTreeNodes(String currentNodeId, WorkflowNode currentNode,
      AbstractFlowNodeBuilder<?, ?> builder, BuildProcessContext context, NodeChildren currentNodeChildren) {
    if (currentNode.getElementType() == WorkflowNodeType.FORM_REPLIED_EVENT || hasFormRepliedEvent(context,
        currentNodeChildren)) {
      return builder;
    }
    // in case of conditional loop, add a default end event
    if (BpmnBuilderHelper.isConditionalLoop(builder, context, currentNodeChildren)) {
      builder.endEvent();
      return builder;
    }
    if (hasLoopAfterSubProcess(context, currentNodeChildren)) {
      builder = BpmnBuilderHelper.endEventSubProcess(context, builder);
    }
    boolean activities = hasActivitiesOnly(context, currentNodeChildren);
    // either child or current node is conditional
    boolean conditional = hasConditionalString(context, currentNodeChildren, currentNodeId);
    builder = addGateway(currentNodeId, builder, activities, conditional);
    return builder;
  }

  private AbstractFlowNodeBuilder<?, ?> addGateway(String currentNodeId, AbstractFlowNodeBuilder<?, ?> builder,
      boolean activities, boolean conditional) {
    // determine the gateway type
    if (activities && conditional) {
      builder = builder.exclusiveGateway(currentNodeId + EXCLUSIVE_GATEWAY_SUFFIX);
    } else if (!activities) {
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
