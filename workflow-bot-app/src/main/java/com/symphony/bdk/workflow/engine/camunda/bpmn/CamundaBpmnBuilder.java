package com.symphony.bdk.workflow.engine.camunda.bpmn;

import static com.symphony.bdk.workflow.engine.camunda.bpmn.BpmnBuilderHelper.hasActivitiesOnly;
import static com.symphony.bdk.workflow.engine.camunda.bpmn.BpmnBuilderHelper.hasAllConditionalChildren;
import static com.symphony.bdk.workflow.engine.camunda.bpmn.BpmnBuilderHelper.hasConditionalString;
import static com.symphony.bdk.workflow.engine.camunda.bpmn.BpmnBuilderHelper.hasLoopAfterSubProcess;

import com.symphony.bdk.core.service.session.SessionService;
import com.symphony.bdk.workflow.engine.WorkflowDirectGraphBuilder;
import com.symphony.bdk.workflow.engine.WorkflowDirectedGraph;
import com.symphony.bdk.workflow.engine.WorkflowDirectedGraph.NodeChildren;
import com.symphony.bdk.workflow.engine.WorkflowNode;
import com.symphony.bdk.workflow.engine.WorkflowNodeType;
import com.symphony.bdk.workflow.engine.camunda.CamundaTranslatedWorkflowContext;
import com.symphony.bdk.workflow.engine.camunda.WorkflowDirectedGraphService;
import com.symphony.bdk.workflow.engine.camunda.bpmn.builder.WorkflowNodeBpmnBuilderFactory;
import com.symphony.bdk.workflow.engine.camunda.variable.VariablesListener;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
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
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Events are created with async before to make sure they are not blocking the dispatch of events (starting or
 * intermediate). This way, 2 workflows listening to the same event are started in parallel
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CamundaBpmnBuilder {
  public static final String DEPLOYMENT_RESOURCE_TOKEN_KEY = "WORKFLOW_TOKEN";
  public static final String EXCLUSIVE_GATEWAY_SUFFIX = "_exclusive_gateway";
  public static final String EVENT_GATEWAY_SUFFIX = "_event_gateway";
  public static final String FORK_GATEWAY = "_fork_gateway";

  private final RepositoryService repositoryService;
  private final WorkflowNodeBpmnBuilderFactory builderFactory;
  private final SessionService sessionService;
  private final WorkflowDirectedGraphService directedGraphService;

  public CamundaTranslatedWorkflowContext translateWorkflow(Workflow workflow)
      throws JsonProcessingException, ModelValidationException {
    CamundaTranslatedWorkflowContext context = workflowToBpmn(workflow);
    try {
      Bpmn.validateModel(context.getBpmnModelInstance());
      log.debug("workflow [{}] has been successfully validated.", workflow.getId());
      return context;
    } finally {
      log.debug("workflow [{}]'s validation is done.", workflow.getId());
      if (log.isDebugEnabled()) {
        WorkflowDebugger.generateDebugFiles(workflow.getId(), context.getBpmnModelInstance());
      }
    }
  }

  public Deployment deployWorkflow(CamundaTranslatedWorkflowContext context) {
    Workflow workflow = context.getWorkflow();
    BpmnModelInstance instance = context.getBpmnModelInstance();
    DeploymentBuilder deploymentBuilder = repositoryService.createDeployment()
        .name(workflow.getId())
        .addModelInstance(workflow.getId() + ".bpmn", instance);
    directedGraphService.putDirectedGraph(context.getWorkflowDirectedGraph());
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

  private CamundaTranslatedWorkflowContext workflowToBpmn(Workflow workflow) throws JsonProcessingException {
    // spaces are not supported in BPMN here
    String processId = workflow.getId().replaceAll("\\s+", "");
    ProcessBuilder process = Bpmn.createExecutableProcess(processId).name(workflow.getId());
    Optional.ofNullable(workflow.getVersion()).ifPresent(v -> process.camundaVersionTag(String.valueOf(v)));

    WorkflowDirectedGraph workflowDirectedGraph = new WorkflowDirectGraphBuilder(workflow, sessionService).build();
    BuildProcessContext context = new BuildProcessContext(workflowDirectedGraph, process);
    buildWorkflowInDfs(new NodeChildren(context.getStartEvents()), "", context);
    AbstractFlowNodeBuilder<?, ?> builder = closeUpSubProcessesIfAny(context, context.getLastNodeBuilder());
    BpmnModelInstance instance = builder.done();
    process.addExtensionElement(VariablesListener.create(instance, workflow.getVariables()));
    return new CamundaTranslatedWorkflowContext(workflow, workflowDirectedGraph, instance);
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
    if (currentNodeChildren != null && !currentNodeChildren.isEmpty()) {
      if (currentNodeChildren.getGateway() == WorkflowDirectedGraph.Gateway.PARALLEL) {
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
    if (hasNoExclusiveFormReplyChildren(context, currentNodeChildren)) {
      log.trace("one of [{}] children is a form replied event", currentNodeId);
      return builder;
    }
    // in case of conditional loop, add a default end event
    if (BpmnBuilderHelper.isConditionalLoop(builder, context, currentNodeChildren)) {
      log.trace("there is a conditional loop from the node [{}], add default end event to the gateway loop",
          currentNodeId);
      builder.endEvent();
      return builder;
    }

    if (hasLoopAfterSubProcess(context, currentNodeChildren, currentNodeType)) {
      builder = BpmnBuilderHelper.endEventSubProcess(context, builder);
    }

    boolean activities = hasActivitiesOnly(context, currentNodeChildren);
    // either child or current node is conditional, since the condition can be defined
    // at parent event or activity itself
    boolean conditional = hasConditionalString(context, currentNodeChildren, currentNodeId);
    log.trace("are the children of the node [{}]'s all activities ? [{}], is there any condition in children ? [{}]",
        currentNodeId, activities, conditional);
    builder = addGateway(currentNodeId, builder, activities, conditional, currentNodeChildren.getChildren().size());
    return builder;
  }

  private AbstractFlowNodeBuilder<?, ?> addGateway(String currentNodeId, AbstractFlowNodeBuilder<?, ?> builder,
      boolean activities, boolean conditional, int childrenSize) {
    // determine the gateway type
    if (activities && conditional) {
      log.trace("an exclusive gateway is added follow the node [{}]", currentNodeId);
      builder = builder.exclusiveGateway((currentNodeId.replace("/", "") + EXCLUSIVE_GATEWAY_SUFFIX));
    } else if (!activities && (conditional || childrenSize > 1)) {
      log.trace("an event gateway is added follow the node [{}]", currentNodeId);
      builder = builder.eventBasedGateway().id(currentNodeId + EVENT_GATEWAY_SUFFIX);
    }
    return builder;
  }

  @SuppressWarnings("checkstyle:JavadocTagContinuationIndentation")
  private void leafNode(String currentNodeId, AbstractFlowNodeBuilder<?, ?> camundaBuilder,
      BuildProcessContext context) {
    // if builder is an instance of sub process builder, the node should be already ended {@see SignalNodeBuilder#31},
    // skip the ending
    /*
     * on:
     *   one-of:
     *     - form-replied:
     *         form-id: init
     *         exclusive: true
     *     - message-received:
     *         content: hey
     */
    if (!(camundaBuilder instanceof SubProcessBuilder)) {
      camundaBuilder = camundaBuilder.endEvent();
      context.addNodeBuilder(currentNodeId, camundaBuilder);
      context.addLastNodeBuilder(camundaBuilder);
    }
  }

  private boolean hasNoExclusiveFormReplyChildren(BuildProcessContext context, NodeChildren currentNodeChildren) {
    return currentNodeChildren.getChildren()
        .stream()
        .map(context::readWorkflowNode)
        .anyMatch(WorkflowNode::isNotExclusiveFormReply);
  }
}
