package com.symphony.bdk.workflow.engine.camunda;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.symphony.bdk.workflow.IntegrationTest;
import com.symphony.bdk.workflow.engine.WorkflowDirectedGraph;
import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;
import com.symphony.bdk.workflow.versioning.repository.VersionedWorkflowRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;

import java.time.Instant;

class WorkflowDirectedGraphServiceTest extends IntegrationTest {

  @Autowired VersionedWorkflowRepository repository;
  @Autowired WorkflowDirectedGraphService directedGraphService;
  @Autowired CacheManager cacheManager;

  static String swadl = "id: workflow\n"
      + "activities:\n"
      + "  - do-something:\n"
      + "      id: doIt\n"
      + "      on:\n"
      + "        message-received:\n"
      + "          content: /execute\n"
      + "      my-parameter: abc";

  @AfterEach
  void cleanup() {
    repository.deleteAll();
  }

  @Test
  void getDirectedGraph() {
    VersionedWorkflow workflow = new VersionedWorkflow();
    workflow.setWorkflowId("workflow");
    workflow.setVersion(Instant.now().toEpochMilli());
    workflow.setSwadl(swadl);
    workflow.setPublished(true);
    workflow.setActive(true);
    repository.save(workflow);
    directedGraphService.getDirectedGraph("workflow");
    Cache cache = cacheManager.getCache(WorkflowDirectedGraphService.ACTIVE_WORKFLOW_DIRECTED_GRAPH);
    assertThat(cache).isNotNull();
    WorkflowDirectedGraph directedGraph = cache.get("workflow", WorkflowDirectedGraph.class);
    assertThat(directedGraph).isNotNull();
    cache.clear();
  }

  @Test
  void testGetDirectedGraph() {
    VersionedWorkflow workflow = new VersionedWorkflow();
    workflow.setWorkflowId("workflow");
    long version = Instant.now().toEpochMilli();
    workflow.setVersion(version);
    workflow.setSwadl(swadl);
    workflow.setPublished(true);
    workflow.setActive(true);
    repository.save(workflow);
    directedGraphService.getDirectedGraph("workflow", version);
    Cache cache = cacheManager.getCache(WorkflowDirectedGraphService.WORKFLOW_DIRECTED_GRAPH);
    assertThat(cache).isNotNull();
    WorkflowDirectedGraph directedGraph = cache.get(new SimpleKey("workflow", version), WorkflowDirectedGraph.class);
    assertThat(directedGraph).isNotNull();
    cache.clear();
  }

  @Test
  void putDirectedGraph() {
    VersionedWorkflow workflow = new VersionedWorkflow();
    workflow.setWorkflowId("workflow");
    long version = Instant.now().toEpochMilli();
    workflow.setVersion(version);
    workflow.setSwadl(swadl);
    workflow.setPublished(true);
    workflow.setActive(true);
    repository.save(workflow);
    WorkflowDirectedGraph graph = directedGraphService.getDirectedGraph("workflow");
    assertThat(graph.getWorkflowId()).isEqualTo("workflow");
    assertThat(graph.getVersion()).isEqualTo(version);

    long newVersion = Instant.now().toEpochMilli();
    WorkflowDirectedGraph newGraph = new WorkflowDirectedGraph("workflow", newVersion);
    directedGraphService.putDirectedGraph(newGraph);
    graph = directedGraphService.getDirectedGraph("workflow");
    assertThat(graph.getVersion()).isEqualTo(newVersion);
    Cache cache = cacheManager.getCache(WorkflowDirectedGraphService.ACTIVE_WORKFLOW_DIRECTED_GRAPH);
    assertThat(cache).isNotNull();
    cache.clear();
  }
}
