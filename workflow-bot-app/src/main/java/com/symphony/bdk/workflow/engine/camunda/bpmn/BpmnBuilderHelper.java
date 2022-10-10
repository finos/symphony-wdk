package com.symphony.bdk.workflow.engine.camunda.bpmn;

import com.symphony.bdk.workflow.engine.WorkflowDirectGraph;
import com.symphony.bdk.workflow.engine.WorkflowNode;
import com.symphony.bdk.workflow.engine.WorkflowNodeType;

import lombok.experimental.UtilityClass;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.builder.AbstractGatewayBuilder;
import org.camunda.bpm.model.bpmn.builder.SubProcessBuilder;

/**
 * Helper class on checks or common actions
 */
@UtilityClass
public class BpmnBuilderHelper {

  public static AbstractFlowNodeBuilder<?, ?> endEventSubProcess(BuildProcessContext context,
      AbstractFlowNodeBuilder<?, ?> builder) {
    builder.endEvent();
    builder = context.removeLastEventSubProcessBuilder().subProcessDone();
    context.cacheSubProcessTimeoutToDone(((SubProcessBuilder) builder));
    return builder;
  }

  public static boolean hasActivitiesOnly(BuildProcessContext context,
      WorkflowDirectGraph.NodeChildren currentNodeChildren) {
    return currentNodeChildren.getChildren()
        .stream()
        .noneMatch(s -> {
          WorkflowNode workflowNode = context.readWorkflowNode(s);
          return workflowNode.getElementType() == WorkflowNodeType.SIGNAL_EVENT
              || workflowNode.getElementType() == WorkflowNodeType.FORM_REPLIED_EVENT;
        });
  }

  public static boolean hasAllConditionalChildren(BuildProcessContext context,
      WorkflowDirectGraph.NodeChildren currentNodeChildren, String parentId) {
    return currentNodeChildren.getChildren()
        .stream()
        .allMatch(s -> context.readWorkflowNode(s).isConditional(parentId));
  }

  public static boolean hasConditionalString(BuildProcessContext context,
      WorkflowDirectGraph.NodeChildren currentNodeChildren, String parentId) {
    return currentNodeChildren.getChildren()
        .stream()
        .anyMatch(s -> context.readWorkflowNode(s).isConditional(parentId));
  }

  public static boolean isConditionalLoop(AbstractFlowNodeBuilder<?, ?> builder, BuildProcessContext context,
      WorkflowDirectGraph.NodeChildren currentNodeChildren) {
    return currentNodeChildren.isChildUnique() && context.isAlreadyBuilt(currentNodeChildren.getUniqueChild())
        && !context.readWorkflowNode(currentNodeChildren.getUniqueChild()).isConditional()
        && builder instanceof AbstractGatewayBuilder;
  }

  public static boolean hasLoopAfterSubProcess(BuildProcessContext context,
      WorkflowDirectGraph.NodeChildren currentNodeChildren,
      WorkflowNodeType currentNodeType) {
    return currentNodeType == WorkflowNodeType.ACTIVITY_COMPLETED_EVENT && context.hasEventSubProcess()
        && currentNodeChildren.getChildren().stream().anyMatch(context::isAlreadyBuilt);
  }
}
