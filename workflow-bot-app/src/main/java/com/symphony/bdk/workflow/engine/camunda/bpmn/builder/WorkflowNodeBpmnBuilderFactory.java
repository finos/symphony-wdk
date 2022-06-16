package com.symphony.bdk.workflow.engine.camunda.bpmn.builder;

import com.symphony.bdk.workflow.engine.WorkflowNode;
import com.symphony.bdk.workflow.engine.WorkflowNodeType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class WorkflowNodeBpmnBuilderFactory {
  private final Map<WorkflowNodeType, WorkflowNodeBpmnBuilder> factory;

  public WorkflowNodeBpmnBuilderFactory(@Autowired List<WorkflowNodeBpmnBuilder> builders) {
    factory = new EnumMap<>(WorkflowNodeType.class);
    builders.forEach(builder -> factory.put(builder.type(), builder));
  }

  public WorkflowNodeBpmnBuilder getBuilder(WorkflowNode element) {
    return factory.get(element.getElementType());
  }
}
