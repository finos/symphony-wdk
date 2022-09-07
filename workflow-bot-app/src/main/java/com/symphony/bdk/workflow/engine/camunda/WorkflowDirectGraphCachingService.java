package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.workflow.engine.WorkflowDirectGraph;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.springframework.stereotype.Component;

@Component
public class WorkflowDirectGraphCachingService {

  private final LoadingCache<String, WorkflowDirectGraph> cache = Caffeine.newBuilder().build(this::getDirectGraph);

  public WorkflowDirectGraph getDirectGraph(String id) {
    return cache.getIfPresent(id);
  }

  public void putDirectGraph(String workflowId, WorkflowDirectGraph workflowDirectGraph) {
    cache.put(workflowId, workflowDirectGraph);
  }
}
