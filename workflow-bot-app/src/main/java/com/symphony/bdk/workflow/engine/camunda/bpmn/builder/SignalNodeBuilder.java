package com.symphony.bdk.workflow.engine.camunda.bpmn.builder;

import com.symphony.bdk.workflow.engine.WorkflowNode;
import com.symphony.bdk.workflow.engine.WorkflowNodeType;
import com.symphony.bdk.workflow.engine.camunda.bpmn.BuildProcessContext;

import org.camunda.bpm.model.bpmn.builder.AbstractCatchEventBuilder;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.builder.AbstractGatewayBuilder;
import org.springframework.stereotype.Component;

@Component
public class SignalNodeBuilder extends AbstractNodeBpmnBuilder {

  @Override
  public AbstractFlowNodeBuilder<?, ?> build(WorkflowNode element, AbstractFlowNodeBuilder<?, ?> builder,
      BuildProcessContext context) {
    if (builder instanceof AbstractCatchEventBuilder) {
      builder = ((AbstractCatchEventBuilder<?, ?>) builder).camundaAsyncBefore()
          .signal(element.getId())
          .name(element.getId());
    } else if (builder instanceof AbstractGatewayBuilder) {
      builder = builder.intermediateCatchEvent(element.getId()).name(element.getId());
    }
    return builder;
  }

  @Override
  public WorkflowNodeType type() {
    return WorkflowNodeType.SIGNAL_EVENT;
  }
}
