package com.symphony.bdk.workflow.engine.camunda.bpmn.builder;

import com.symphony.bdk.workflow.engine.WorkflowNode;
import com.symphony.bdk.workflow.engine.WorkflowNodeType;
import com.symphony.bdk.workflow.engine.camunda.bpmn.BuildProcessContext;
import com.symphony.bdk.workflow.swadl.v1.EventWithTimeout;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.builder.EventSubProcessBuilder;
import org.springframework.stereotype.Component;

@Component
public class ActivityExpiredNodeBuilder extends ActivityNodeBuilder {

  @Override
  public AbstractFlowNodeBuilder<?, ?> build(WorkflowNode element, AbstractFlowNodeBuilder<?, ?> builder,
      BuildProcessContext context)
      throws JsonProcessingException {
    if (context.getParents(element.getId())
        .stream()
        .map(context::readWorkflowNode)
        .anyMatch(node -> node.getElementType() == WorkflowNodeType.FORM_REPLIED_EVENT)) {
      EventSubProcessBuilder subProcess = context.removeLastSubProcessBuilder();
      builder = subProcess.subProcessDone();
      return addTask(builder, element.getActivity(), context);
    }
    String timeout = ((EventWithTimeout) element.getEvent()).getTimeout();
    builder = builder.intermediateCatchEvent().timerWithDuration(timeout);
    return builder;
  }

  @Override
  public WorkflowNodeType type() {
    return WorkflowNodeType.ACTIVITY_EXPIRED_EVENT;
  }
}
