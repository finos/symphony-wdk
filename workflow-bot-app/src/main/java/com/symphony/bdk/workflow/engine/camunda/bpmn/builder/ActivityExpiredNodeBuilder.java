package com.symphony.bdk.workflow.engine.camunda.bpmn.builder;

import com.symphony.bdk.workflow.engine.WorkflowNode;
import com.symphony.bdk.workflow.engine.WorkflowNodeType;
import com.symphony.bdk.workflow.engine.camunda.bpmn.BuildProcessContext;
import com.symphony.bdk.workflow.swadl.v1.EventWithTimeout;

import org.camunda.bpm.model.bpmn.builder.AbstractCatchEventBuilder;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.builder.AbstractGatewayBuilder;
import org.camunda.bpm.model.bpmn.builder.SubProcessBuilder;
import org.springframework.stereotype.Component;

@Component
public class ActivityExpiredNodeBuilder extends ActivityNodeBuilder {

  @Override
  public AbstractFlowNodeBuilder<?, ?> build(WorkflowNode element, String parentId,
      AbstractFlowNodeBuilder<?, ?> builder, BuildProcessContext context) {
    if (hasNoExclusiveFormReplyParent(element, context)) {
      if (context.hasTimeoutSubProcess()) {
        builder = context.removeLastSubProcessTimeoutBuilder();
      } else {
        // the previous subprocess is not closed yet, close it and extend with timeout boundary event
        builder = context.removeLastEventSubProcessBuilder().subProcessDone();
        builder = ((SubProcessBuilder) builder).boundaryEvent().error(ERROR_CODE);
      }
      return addTask(builder, element.getActivity());
    }
    String timeout = ((EventWithTimeout) element.getEvent()).getTimeout();
    if (builder instanceof AbstractCatchEventBuilder) {
      builder = ((AbstractCatchEventBuilder<?, ?>) builder).timerWithDuration(timeout);
    } else if (builder instanceof AbstractGatewayBuilder) {
      builder = builder.intermediateCatchEvent().name(element.getId()).timerWithDuration(timeout);
    }
    return builder;
  }

  private boolean hasNoExclusiveFormReplyParent(WorkflowNode element, BuildProcessContext context) {
    return context.getParents(element.getId())
        .stream()
        .map(context::readWorkflowNode)
        .anyMatch(WorkflowNode::isNotExclusiveFormReply);
  }

  @Override
  public WorkflowNodeType type() {
    return WorkflowNodeType.ACTIVITY_EXPIRED_EVENT;
  }
}
