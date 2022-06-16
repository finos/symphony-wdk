package com.symphony.bdk.workflow.engine.camunda.bpmn.builder;

import com.symphony.bdk.workflow.engine.WorkflowNodeType;

import org.springframework.stereotype.Component;

@Component
public class ActivityFailedNodeBuilder extends ActivityNodeBuilder {

  @Override
  public WorkflowNodeType type() {
    return WorkflowNodeType.ACTIVITY_FAILED_EVENT;
  }
}
