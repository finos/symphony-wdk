package com.symphony.bdk.workflow.engine.camunda.bpmn;

import static com.symphony.bdk.workflow.engine.camunda.bpmn.builder.WorkflowNodeBpmnBuilder.ERROR_CODE;

import com.symphony.bdk.workflow.engine.WorkflowDirectGraph;

import lombok.experimental.Delegate;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import org.camunda.bpm.model.bpmn.builder.EventSubProcessBuilder;
import org.camunda.bpm.model.bpmn.builder.ProcessBuilder;
import org.camunda.bpm.model.bpmn.builder.SubProcessBuilder;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * A context holder class, having the cached builders and graph relationships in one place.
 * This is needed during the camunda bpmn construction process.
 */
public class BuildProcessContext {
  private static final String LAST = "last";
  @Delegate
  private final WorkflowDirectGraph workflowGraph;
  private final Map<String, AbstractFlowNodeBuilder<?, ?>> camundaBuilderMap = new HashMap<>();

  /**
   * cache the subprocess builders it is useful only when an activity has a one-of event list,
   * which contains a form reply event inside, we have to make all events as event subprocess
   * in the same subprocess.
   */
  private final Deque<SubProcessBuilder> subProcessBuilders = new LinkedList<>();

  /**
   * cache the form reply event sub process builder, when sub process is finish, we have to close
   * the builder
   */
  private final Deque<EventSubProcessBuilder> eventSubProcessBuilders = new LinkedList<>();

  /**
   * cache the timeout flow inside a form reply sub process, if there is no follow up expired events,
   * we close the builder at the end, otherwise, these expired events are connected with the cached
   * builder in this list.
   */
  private final Deque<SubProcessBuilder> subProcessTimeoutBuilders = new LinkedList<>();

  /**
   * Used to start the entire workflow process, especially when a workflow has multiple start events.
   */
  private final ProcessBuilder processBuilder;

  public BuildProcessContext(WorkflowDirectGraph workflowGraph, ProcessBuilder process) {
    this.workflowGraph = workflowGraph;
    this.processBuilder = process;
  }

  public void addLastNodeBuilder(AbstractFlowNodeBuilder<?, ?> builder) {
    camundaBuilderMap.put(LAST, builder);
  }

  public AbstractFlowNodeBuilder<?, ?> getLastNodeBuilder() {
    return camundaBuilderMap.get(LAST) == null ? camundaBuilderMap.get("") : camundaBuilderMap.get(LAST);
  }

  public void addNodeBuilder(String nodeId, AbstractFlowNodeBuilder<?, ?> builder) {
    camundaBuilderMap.put(nodeId, builder);
  }

  public AbstractFlowNodeBuilder<?, ?> getNodeBuilder(String nodeId) {
    if (StringUtils.isEmpty(nodeId)) {
      AbstractFlowNodeBuilder<?, ?> builder = this.processBuilder.startEvent();
      camundaBuilderMap.put("", builder);
      return builder;
    }
    return camundaBuilderMap.get(nodeId);
  }

  public boolean isAlreadyBuilt(String nodeId) {
    return camundaBuilderMap.containsKey(nodeId);
  }

  public void cacheSubProcess(SubProcessBuilder builder) {
    subProcessBuilders.add(builder);
  }

  public void cacheEventSubProcessToDone(EventSubProcessBuilder builder) {
    eventSubProcessBuilders.add(builder);
  }

  public void cacheSubProcessTimeoutToDone(SubProcessBuilder builder) {
    subProcessTimeoutBuilders.add(builder);
  }

  public SubProcessBuilder getLastSubProcessBuilder() {
    return subProcessBuilders.getLast();
  }

  public EventSubProcessBuilder removeLastEventSubProcessBuilder() {
    return eventSubProcessBuilders.removeLast();
  }

  public AbstractFlowNodeBuilder<?, ?> removeLastSubProcessTimeoutBuilder() {
    return subProcessTimeoutBuilders.removeLast().boundaryEvent().error(ERROR_CODE);
  }

  public boolean hasEventSubProcess() {
    return !eventSubProcessBuilders.isEmpty();
  }

  public boolean hasTimeoutSubProcess() {
    return !subProcessTimeoutBuilders.isEmpty();
  }
}
