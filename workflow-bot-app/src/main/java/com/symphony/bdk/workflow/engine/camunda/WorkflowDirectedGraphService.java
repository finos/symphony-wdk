package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.core.service.session.SessionService;
import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.engine.WorkflowDirectGraphBuilder;
import com.symphony.bdk.workflow.engine.WorkflowDirectedGraph;
import com.symphony.bdk.workflow.management.repository.VersionedWorkflowRepository;
import com.symphony.bdk.workflow.management.repository.domain.VersionedWorkflow;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowDirectedGraphService {
  public static final String WORKFLOW_DIRECTED_GRAPH = "WORKFLOW_DIRECTED_GRAPH";
  public static final String ACTIVE_WORKFLOW_DIRECTED_GRAPH = "ACTIVE_WORKFLOW_DIRECTED_GRAPH";

  private final Optional<VersionedWorkflowRepository> versionedWorkflowRepository;
  private final SessionService sessionService;
  private final ObjectConverter objectConverter;

  @Cacheable(value = ACTIVE_WORKFLOW_DIRECTED_GRAPH)
  public WorkflowDirectedGraph getDirectedGraph(String id) {
    log.debug("Workflow [{}] is not missing in cache", id);
    if (versionedWorkflowRepository.isEmpty()) {
      return null;
    }
    log.debug("Hit DB to retrieve workflow [{}] from DB and convert to directed graph", id);
    return mapToDirectedGraph(() -> versionedWorkflowRepository.get().findByWorkflowIdAndActiveTrue(id));
  }

  @Cacheable(WORKFLOW_DIRECTED_GRAPH)
  public WorkflowDirectedGraph getDirectedGraph(String id, Long version) {
    log.debug("Workflow [{}] is not missing in cache", id);
    if (versionedWorkflowRepository.isEmpty()) {
      return null;
    }
    log.debug("Hit DB to retrieve workflow [{}] version [{}] from DB and convert to directed graph", id, version);
    return mapToDirectedGraph(() -> versionedWorkflowRepository.get().findByWorkflowIdAndVersion(id, version));
  }

  private WorkflowDirectedGraph mapToDirectedGraph(Supplier<Optional<VersionedWorkflow>> supplier) {
    Optional<VersionedWorkflow> workflow = supplier.get();
    WorkflowDirectedGraph directedGraph =
        workflow.map(w -> objectConverter.convert(w.getSwadl(), w.getVersion(), Workflow.class))
            .map(w -> new WorkflowDirectGraphBuilder(w, sessionService).build()).orElse(null);
    log.trace("Converted directed graph = [{}]", directedGraph);
    return directedGraph;
  }

  @CachePut(value = ACTIVE_WORKFLOW_DIRECTED_GRAPH, key = "#directedGraph.workflowId")
  public WorkflowDirectedGraph putDirectedGraph(WorkflowDirectedGraph directedGraph) {
    return directedGraph;
  }
}
