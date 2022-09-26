package com.symphony.bdk.workflow.engine.camunda.bpmn.builder;

import com.symphony.bdk.workflow.engine.WorkflowDirectGraph;
import com.symphony.bdk.workflow.engine.WorkflowNode;
import com.symphony.bdk.workflow.engine.WorkflowNodeType;
import com.symphony.bdk.workflow.engine.camunda.bpmn.BuildProcessContext;

import org.camunda.bpm.model.bpmn.builder.AbstractCatchEventBuilder;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.builder.AbstractGatewayBuilder;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("checkstyle:JavadocTagContinuationIndentation")
public class SignalNodeBuilder extends AbstractNodeBpmnBuilder {

  @Override
  public AbstractFlowNodeBuilder<?, ?> build(WorkflowNode element, String parentId,
      AbstractFlowNodeBuilder<?, ?> builder, BuildProcessContext context) {
    // this signal has a form replied event brother, they share the same parent, they also share the same child
    // activity, therefore end this signal event right away
    if (hasFormRepliedEventBrother(context, parentId)) {
      builder = context.getLastSubProcessBuilder()
          .embeddedSubProcess()
          .eventSubProcess()
          .startEvent()
          .camundaAsyncBefore()
          .interrupting(true)
          .message(element.getId())
          .name(element.getId())
          .endEvent().subProcessDone();
    } else if (builder instanceof AbstractCatchEventBuilder) {
      builder = ((AbstractCatchEventBuilder<?, ?>) builder).camundaAsyncBefore()
          .signal(element.getId())
          .name(element.getId());
    } else if (builder instanceof AbstractGatewayBuilder) {
      builder = builder.intermediateCatchEvent().camundaAsyncBefore().signal(element.getId()).name(element.getId());
    }
    return builder;
  }

  /**
   * @formatter:off
   * test if the current signal event has a form reply event brother.
   * e.g.
   * on:
   *   one-of:
   *     - form-replied:
   *         form-id: init
   *         exclusive: true
   *     - message-received:
   *  @formatter:on
   */
  private boolean hasFormRepliedEventBrother(BuildProcessContext context, String parentId) {
    return context.readChildren(parentId) != null
        && context.readChildren(parentId).getGateway() != WorkflowDirectGraph.Gateway.PARALLEL && context.readChildren(
            parentId).getChildren()
        .stream()
        .anyMatch(s -> context.readWorkflowNode(s).getElementType() == WorkflowNodeType.FORM_REPLIED_EVENT);
  }

  @Override
  public WorkflowNodeType type() {
    return WorkflowNodeType.SIGNAL_EVENT;
  }
}
