package com.symphony.bdk.workflow.engine.camunda.bpmn.builder;

import com.symphony.bdk.workflow.engine.WorkflowNode;
import com.symphony.bdk.workflow.engine.WorkflowNodeType;
import com.symphony.bdk.workflow.engine.camunda.bpmn.BuildProcessContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.springframework.stereotype.Component;

@Component
public class JoinActivityNodeBuilder extends AbstractNodeBpmnBuilder {
  @Override
  protected AbstractFlowNodeBuilder<?, ?> build(WorkflowNode element, String parentId,
      AbstractFlowNodeBuilder<?, ?> builder, BuildProcessContext context) throws JsonProcessingException {
    return builder.parallelGateway(element.getId());
  }

  @Override
  public WorkflowNodeType type() {
    return WorkflowNodeType.JOIN_ACTIVITY;
  }
}
