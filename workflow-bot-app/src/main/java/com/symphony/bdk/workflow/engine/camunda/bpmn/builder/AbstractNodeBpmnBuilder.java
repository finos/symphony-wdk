package com.symphony.bdk.workflow.engine.camunda.bpmn.builder;

import static com.symphony.bdk.workflow.engine.camunda.bpmn.BpmnBuilderHelper.endEventSubProcess;
import static com.symphony.bdk.workflow.engine.camunda.bpmn.CamundaBpmnBuilder.EXCLUSIVE_GATEWAY_SUFFIX;

import com.symphony.bdk.workflow.engine.WorkflowNode;
import com.symphony.bdk.workflow.engine.camunda.bpmn.BuildProcessContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.builder.AbstractGatewayBuilder;
import org.camunda.bpm.model.bpmn.builder.SubProcessBuilder;

public abstract class AbstractNodeBpmnBuilder implements WorkflowNodeBpmnBuilder {

  @Override
  public AbstractFlowNodeBuilder<?, ?> connect(WorkflowNode element, String parentId,
      AbstractFlowNodeBuilder<?, ?> builder, BuildProcessContext context) throws JsonProcessingException {
    String nodeId = element.getId();
    if (context.isAlreadyBuilt(nodeId)) {
      if (element.isConditional()) {
        builder = doConditionalConnection(element, parentId, builder, context, nodeId);
      } else {
        connectToExistingNode(nodeId, builder);
      }
      return builder;
    } else {
      if (builder instanceof AbstractGatewayBuilder && element.isConditional()) {
        builder = builder.condition("if", element.getIfCondition(parentId));
      }
      // in case where the current activity has multiple parents (using one-of events),
      // and one of them is a form replied event. We have to end the form reply sub process first,
      // then connect the activity
      if (context.hasEventSubProcess() && context.getParents(element.getId()).size() > 1) {
        builder = endEventSubProcess(context, builder);
      }
      return build(element, parentId, builder, context);
    }
  }

  private AbstractFlowNodeBuilder<?, ?> doConditionalConnection(WorkflowNode element, String parentId,
      AbstractFlowNodeBuilder<?, ?> builder, BuildProcessContext context, String nodeId) {
    if (!(builder instanceof AbstractGatewayBuilder)) {
      if (context.hasEventSubProcess()) {
        builder = endEventSubProcess(context, builder);
      }
      builder.exclusiveGateway(nodeId + EXCLUSIVE_GATEWAY_SUFFIX)
          .condition("if", element.getIfCondition(parentId))
          .connectTo(nodeId)
          .moveToNode(nodeId + EXCLUSIVE_GATEWAY_SUFFIX)
          .endEvent();
    } else {
      builder.condition("if", element.getIfCondition(parentId))
          .connectTo(nodeId);
    }
    return builder;
  }

  protected void connectToExistingNode(String nodeId, AbstractFlowNodeBuilder<?, ?> builder) {
    if (!(builder instanceof SubProcessBuilder)) {
      builder.connectTo(nodeId);
    }
  }

  protected abstract AbstractFlowNodeBuilder<?, ?> build(WorkflowNode element, String parentId,
      AbstractFlowNodeBuilder<?, ?> builder, BuildProcessContext context) throws JsonProcessingException;
}
