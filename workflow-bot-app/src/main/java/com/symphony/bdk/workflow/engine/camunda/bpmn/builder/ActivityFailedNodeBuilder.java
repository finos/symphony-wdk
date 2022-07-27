package com.symphony.bdk.workflow.engine.camunda.bpmn.builder;

import com.symphony.bdk.workflow.engine.WorkflowNode;
import com.symphony.bdk.workflow.engine.WorkflowNodeType;
import com.symphony.bdk.workflow.engine.camunda.bpmn.BuildProcessContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.camunda.bpm.model.bpmn.builder.AbstractActivityBuilder;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.springframework.stereotype.Component;

@Component
public class ActivityFailedNodeBuilder extends ActivityNodeBuilder {

  @Override
  protected void connectToExistingNode(String nodeId, AbstractFlowNodeBuilder<?, ?> builder) {
    ((AbstractActivityBuilder<?, ?>) builder).boundaryEvent()
        .name("error_" + nodeId)
        .error().connectTo(nodeId);
  }

  @Override
  public AbstractFlowNodeBuilder<?, ?> build(WorkflowNode element, String parentId,
      AbstractFlowNodeBuilder<?, ?> builder, BuildProcessContext context) throws JsonProcessingException {
    builder = ((AbstractActivityBuilder<?, ?>) builder).boundaryEvent()
        .name("error_" + element.getId())
        .error();
    return addTask(builder, element.getActivity(), context);
  }

  @Override
  public WorkflowNodeType type() {
    return WorkflowNodeType.ACTIVITY_FAILED_EVENT;
  }
}
