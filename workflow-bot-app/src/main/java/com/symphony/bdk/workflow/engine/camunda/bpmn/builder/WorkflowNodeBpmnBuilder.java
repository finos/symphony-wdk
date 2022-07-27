package com.symphony.bdk.workflow.engine.camunda.bpmn.builder;

import com.symphony.bdk.workflow.engine.WorkflowNode;
import com.symphony.bdk.workflow.engine.WorkflowNodeType;
import com.symphony.bdk.workflow.engine.camunda.bpmn.BuildProcessContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;

public interface WorkflowNodeBpmnBuilder {
  String ERROR_CODE = "408";

  AbstractFlowNodeBuilder<?, ?> connect(WorkflowNode element, String parentId, AbstractFlowNodeBuilder<?, ?> builder,
      BuildProcessContext context) throws JsonProcessingException;

  WorkflowNodeType type();
}
