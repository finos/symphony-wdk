package com.symphony.bdk.workflow.engine.camunda.bpmn;

import com.symphony.bdk.workflow.engine.WorkflowDirectGraph;

import lombok.experimental.Delegate;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.builder.EventSubProcessBuilder;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class BuildProcessContext {
  private static final String LAST = "last";
  @Delegate
  private final WorkflowDirectGraph workflowGraph;
  private final Map<String, AbstractFlowNodeBuilder<?, ?>> camundaBuilderMap = new HashMap<>();

  private final LinkedList<EventSubProcessBuilder> subProcessBuilders = new LinkedList<>();

  public BuildProcessContext(WorkflowDirectGraph workflowGraph) {
    this.workflowGraph = workflowGraph;
  }

  public void addLastNodeBuilder(AbstractFlowNodeBuilder<?, ?> builder) {
    camundaBuilderMap.put(LAST, builder);
  }

  public AbstractFlowNodeBuilder<?, ?> getLastNodeBuilder() {
    return camundaBuilderMap.containsKey(LAST) ? camundaBuilderMap.get(LAST) : camundaBuilderMap.get("");
  }

  public void addNodeBuilder(String nodeId, AbstractFlowNodeBuilder<?, ?> builder) {
    camundaBuilderMap.put(nodeId, builder);
  }

  public AbstractFlowNodeBuilder<?, ?> getNodeBuilder(String nodeId) {
    return camundaBuilderMap.get(nodeId);
  }

  public boolean isAlreadyBuilt(String nodeId) {
    return camundaBuilderMap.containsKey(nodeId);
  }

  public void addSubProcessToBeDone(EventSubProcessBuilder builder) {
    subProcessBuilders.add(builder);
  }

  public EventSubProcessBuilder removeLastSubProcessBuilder() {
    return subProcessBuilders.removeLast();
  }

  public boolean hasSubProcess() {
    return !subProcessBuilders.isEmpty();
  }
}
