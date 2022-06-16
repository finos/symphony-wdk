package com.symphony.bdk.workflow.engine.camunda.bpmn;

import com.symphony.bdk.workflow.engine.WorkflowDirectGraph;
import com.symphony.bdk.workflow.engine.WorkflowDirectGraph.NodeChildren;
import com.symphony.bdk.workflow.engine.WorkflowDirectGraphBuilder;
import com.symphony.bdk.workflow.engine.WorkflowNode;
import com.symphony.bdk.workflow.engine.WorkflowNodeType;
import com.symphony.bdk.workflow.engine.camunda.WorkflowEventToCamundaEvent;
import com.symphony.bdk.workflow.engine.camunda.bpmn.builder.WorkflowNodeBpmnBuilderFactory;
import com.symphony.bdk.workflow.engine.camunda.variable.VariablesListener;
import com.symphony.bdk.workflow.swadl.v1.EventWithTimeout;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.DeploymentBuilder;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.builder.AbstractGatewayBuilder;
import org.camunda.bpm.model.bpmn.builder.ProcessBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Events are created with async before to make sure they are not blocking the dispatch of events (starting or
 * intermediate). This way, 2 workflows listening to the same event are started in parallel
 */
@Slf4j
@Component
public class CamundaBpmnBuilder {
  public static final String DEPLOYMENT_RESOURCE_TOKEN_KEY = "WORKFLOW_TOKEN";

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

  private void buildWorkflowInDfs(NodeChildren nodes, String parentNodeId, BuildProcessContext context)
      throws JsonProcessingException {
    List<String> children = nodes.getChildren();
    for (String currentNodeId : children) {
      WorkflowNode currentNode = context.readWorkflowNode(currentNodeId);
      AbstractFlowNodeBuilder<?, ?> camundaBuilder = context.getNodeBuilder(parentNodeId);
      if (context.isAlreadyBuilt(currentNodeId)) {
        if (currentNode.isConditional()) {
          /* shit code */
          if (!(camundaBuilder instanceof AbstractGatewayBuilder)) {
            camundaBuilder.exclusiveGateway(currentNodeId + "_exclusive_gateway")
                .condition("if", currentNode.getIfCondition(parentNodeId))
                .connectTo(currentNodeId)
                .moveToNode(currentNodeId + "_exclusive_gateway")
                .endEvent();
          } else {
            camundaBuilder.condition("if", currentNode.getIfCondition(parentNodeId))
                .connectTo(currentNodeId);
          }
          /* end shit */
        } else {
          camundaBuilder.connectTo(currentNodeId);
        }
        continue;
      } else {
        if (camundaBuilder instanceof AbstractGatewayBuilder && currentNode.isConditional()) {
          camundaBuilder = camundaBuilder.condition("if", currentNode.getIfCondition(parentNodeId));
        }
        camundaBuilder =
            builderFactory.getBuilder(currentNode).build(currentNode, camundaBuilder, context);
      }
      computeChildren(currentNodeId, currentNode, camundaBuilder, context);
    }
  }

  private void computeChildren(String currentNodeId, WorkflowNode currentNode,
      AbstractFlowNodeBuilder<?, ?> camundaBuilder, BuildProcessContext context)
      throws JsonProcessingException {
    NodeChildren currentNodeChildren = context.readChildren(currentNode.getId());
    if (currentNodeChildren != null) {
      if ((currentNode.getElementType() != WorkflowNodeType.FORM_REPLIED_EVENT || isExclusiveFormRepliedNode(
          currentNode)) && !currentNodeChildren.isChildUnique()) {
        switch (currentNodeChildren.getGateway()) {
          case EVENT_BASED:
            camundaBuilder = camundaBuilder.eventBasedGateway().id(currentNodeId + "_event_gateway");
            break;
          case EXCLUSIVE:
            camundaBuilder = camundaBuilder.exclusiveGateway(currentNodeId + "_exclusive_gateway");
        }
      }
      /*   this is a shit */
      else if (currentNodeChildren.isChildUnique() && isExclusiveFormRepliedNode(
          context.readWorkflowNode(currentNodeChildren.getUniqueChild()))) {
        camundaBuilder = camundaBuilder.eventBasedGateway().id(currentNodeId + "_event_gateway");
        currentNodeChildren.gateway(WorkflowDirectGraph.Gateway.EVENT_BASED);
        String newTimeoutEvent = currentNodeId + "_timeout";
        currentNodeChildren.addChild(newTimeoutEvent);

        EventWithTimeout timeoutEvent = new EventWithTimeout();
        timeoutEvent.setTimeout("PT24H");
        context.registerToDictionary(newTimeoutEvent, new WorkflowNode().id(newTimeoutEvent)
            .event(timeoutEvent)
            .elementType(WorkflowNodeType.ACTIVITY_EXPIRED_EVENT));
        context.addParent(newTimeoutEvent, currentNodeId);
        /* end shit */
      } else if (
          currentNodeChildren.isChildUnique() && context.isAlreadyBuilt(
              currentNodeChildren.getUniqueChild())) {
        // in case of conditional loop, add a default end event
        if (!context.readWorkflowNode(currentNodeChildren.getUniqueChild()).isConditional()) {
          camundaBuilder.endEvent();
        }
      }
      context.addNodeBuilder(currentNodeId, camundaBuilder);
      buildWorkflowInDfs(currentNodeChildren, currentNodeId, context);
    } else {
      camundaBuilder = camundaBuilder.endEvent();
      context.addLastNodeBuilder(camundaBuilder);
    }
  }

  private boolean isExclusiveFormRepliedNode(WorkflowNode currentNode) {
    return currentNode.getElementType() == WorkflowNodeType.FORM_REPLIED_EVENT && currentNode.getEvent()
        .getFormReplied()
        .getExclusive();
  }

  private BpmnModelInstance workflowToBpmn(Workflow workflow) throws JsonProcessingException {
    // spaces are not supported in BPMN here
    String processId = workflow.getId().replaceAll("\\s+", "");
    ProcessBuilder process = Bpmn.createExecutableProcess(processId).name(workflow.getId());

    WorkflowDirectGraphBuilder workflowDirectGraphBuilder = new WorkflowDirectGraphBuilder(workflow, eventToMessage);
    WorkflowDirectGraph workflowDirectGraph = workflowDirectGraphBuilder.build();
    BuildProcessContext context = new BuildProcessContext(workflowDirectGraph);

    List<String> startEvents = context.getStartEvents();
    context.addNodeBuilder("", process.startEvent());

    buildWorkflowInDfs(new NodeChildren(startEvents), "", context);

    AbstractFlowNodeBuilder<?, ?> builder = context.getLastNodeBuilder();
    if (context.hasSubProcess()) {
      builder = context.removeLastSubProcessBuilder().subProcessDone().endEvent();
    }
    BpmnModelInstance model = builder.done();
    process.addExtensionElement(VariablesListener.create(model, workflow.getVariables()));
    return model;
  }
}
