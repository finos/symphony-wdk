package com.symphony.bdk.workflow.engine.camunda.bpmn.builder;

import com.symphony.bdk.workflow.engine.WorkflowNode;
import com.symphony.bdk.workflow.engine.WorkflowNodeType;
import com.symphony.bdk.workflow.engine.camunda.bpmn.BuildProcessContext;

import org.camunda.bpm.model.bpmn.builder.AbstractCatchEventBuilder;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.springframework.stereotype.Component;

@Component
public class TimerFiredNodeBuilder extends AbstractNodeBpmnBuilder {

  @Override
  public AbstractFlowNodeBuilder<?, ?> build(WorkflowNode element, AbstractFlowNodeBuilder<?, ?> builder,
      BuildProcessContext context) {
    if (element.getEvent().getTimerFired().getRepeat() != null) {
      builder = ((AbstractCatchEventBuilder<?, ?>) builder)
          .timerWithCycle(element.getEvent().getTimerFired().getRepeat());
    } else {
      builder = ((AbstractCatchEventBuilder<?, ?>) builder)
          .timerWithDate(element.getEvent().getTimerFired().getAt());
    }
    builder = builder.name(element.getId());
    return builder;
  }

  @Override
  public WorkflowNodeType type() {
    return WorkflowNodeType.TIMER_FIRED_EVENT;
  }
}
