package com.symphony.bdk.workflow.monitoring.service;

import static com.symphony.bdk.workflow.engine.WorkflowDirectedGraph.NodeChildren;

import com.symphony.bdk.workflow.api.v1.dto.NodeStateView;
import com.symphony.bdk.workflow.api.v1.dto.NodeView;
import com.symphony.bdk.workflow.api.v1.dto.StatusEnum;
import com.symphony.bdk.workflow.api.v1.dto.VariableView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstLifeCycleFilter;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowNodesStateView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowNodesView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;
import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.engine.WorkflowDirectedGraph;
import com.symphony.bdk.workflow.engine.WorkflowNode;
import com.symphony.bdk.workflow.engine.WorkflowNodeType;
import com.symphony.bdk.workflow.engine.WorkflowNodeTypeHelper;
import com.symphony.bdk.workflow.engine.camunda.WorkflowDirectedGraphService;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.exception.NotFoundException;
import com.symphony.bdk.workflow.monitoring.repository.ActivityQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.VariableQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.WorkflowInstQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.WorkflowQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.domain.ActivityInstanceDomain;
import com.symphony.bdk.workflow.monitoring.repository.domain.VariablesDomain;
import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowInstanceDomain;
import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;
import com.symphony.bdk.workflow.versioning.repository.VersionedWorkflowRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

@RequiredArgsConstructor
@Component
public class MonitoringService {
  private final WorkflowDirectedGraphService workflowDirectedGraphService;
  private final WorkflowQueryRepository workflowQueryRepository;
  private final WorkflowInstQueryRepository workflowInstQueryRepository;
  private final ActivityQueryRepository activityQueryRepository;
  private final VariableQueryRepository variableQueryRepository;
  private final ObjectConverter objectConverter;

  private final Optional<VersionedWorkflowRepository> optionalVersionedRepository;

  public List<WorkflowView> listAllWorkflows() {
    if (optionalVersionedRepository.isPresent()) {
      VersionedWorkflowRepository repository = optionalVersionedRepository.get();
      List<VersionedWorkflow> activeWorkflows = repository.findByActiveTrue();
      return objectConverter.convertCollection(activeWorkflows, WorkflowView.class);
    }
    return objectConverter.convertCollection(workflowQueryRepository.findAll(), WorkflowView.class);
  }

  public List<WorkflowInstView> listWorkflowInstances(String workflowId, String status, Long version) {
    List<WorkflowInstanceDomain> allById = new ArrayList<>();
    if (status != null) {
      Optional.ofNullable(version)
          .ifPresentOrElse(
              v -> allById.addAll(workflowInstQueryRepository.findAllByIdAndStatusAndVersion(workflowId,
                  StatusEnum.toInstanceStatusEnum(status), String.valueOf(v))),
              () -> allById.addAll(
                  workflowInstQueryRepository.findAllByIdAndStatus(workflowId,
                      StatusEnum.toInstanceStatusEnum(status))));
    } else {
      Optional.ofNullable(version)
          .ifPresentOrElse(
              v -> allById.addAll(workflowInstQueryRepository.findAllByIdAndVersion(workflowId, String.valueOf(v))),
              () -> allById.addAll(workflowInstQueryRepository.findAllById(workflowId)));
    }

    return objectConverter.convertCollection(allById, WorkflowInstView.class);
  }

  public WorkflowNodesStateView listWorkflowInstanceNodes(String workflowId, String instanceId,
      WorkflowInstLifeCycleFilter lifeCycleFilter) {
    // check if the instance belongs to the provided workflow
    WorkflowInstView instance = this.checkIsInstanceOfWorkflow(workflowId, instanceId);

    List<ActivityInstanceDomain> activityInstances =
        activityQueryRepository.findAllByWorkflowInstanceId(workflowId, instanceId, lifeCycleFilter);
    WorkflowDirectedGraph directGraph = getWorkflowDirectedGraph(workflowId, instance.getVersion());

    List<NodeStateView> nodes = getNodeViews(activityInstances, directGraph);
    VariablesDomain globalVariables = this.variableQueryRepository.findVarsByWorkflowInstanceIdAndVarName(instanceId,
        ActivityExecutorContext.VARIABLES);
    VariablesDomain error =
        this.variableQueryRepository.findVarsByWorkflowInstanceIdAndVarName(instanceId, ActivityExecutorContext.ERROR);

    return buildWorkflowNodesStateView(nodes, globalVariables, error);
  }

  private static WorkflowNodesStateView buildWorkflowNodesStateView(List<NodeStateView> nodes,
      VariablesDomain globalVariables, VariablesDomain error) {
    WorkflowNodesStateView result = new WorkflowNodesStateView();
    result.setNodes(nodes.stream().filter(a -> a.getType() != null).collect(Collectors.toList()));
    result.setGlobalVariables(new VariableView(globalVariables));
    if (!error.getOutputs().isEmpty()) {
      result.setError(error.getOutputs());
    }
    return result;
  }

  private List<NodeStateView> getNodeViews(List<ActivityInstanceDomain> activityInstances,
      WorkflowDirectedGraph directGraph) {
    List<NodeStateView> nodes = objectConverter.convertCollection(activityInstances, NodeStateView.class);
    nodes.stream()
        .filter(node -> directGraph.isRegistered(node.getNodeId()))
        .forEach(node -> {
          WorkflowNode workflowNode = directGraph.getDictionary().get(node.getNodeId());
          String name = WorkflowNodeTypeHelper.toUpperUnderscore(workflowNode.getWrappedType().getSimpleName());
          node.setType(WorkflowNodeTypeHelper.toType(name));
          node.setGroup(WorkflowNodeTypeHelper.toGroup(name));
        });
    return nodes;
  }

  public WorkflowNodesView getWorkflowDefinition(String workflowId) {
    return this.getWorkflowDefinition(workflowId, null);
  }

  public WorkflowNodesView getWorkflowDefinition(String workflowId, Long version) {
    WorkflowDirectedGraph directedGraph = getWorkflowDirectedGraph(workflowId, version);

    Map<String, WorkflowNode> dictionary = directedGraph.getDictionary();
    List<NodeView> activities = dictionary.keySet().stream().map(node -> {
      WorkflowNode workflowNode = dictionary.get(node);
      NodeChildren children = directedGraph.getChildren(node);
      List<String> parents = directedGraph.getParents(node);
      return buildNode(dictionary, node, workflowNode, children, parents);
    }).collect(Collectors.toList());

    return WorkflowNodesView.builder()
        .workflowId(workflowId)
        .flowNodes(activities)
        .version(directedGraph.getVersion())
        .variables(directedGraph.getVariables()).build();
  }

  private WorkflowDirectedGraph getWorkflowDirectedGraph(String workflowId, Long version) {
    WorkflowDirectedGraph directGraph = Optional.ofNullable(version)
        .map(v -> this.workflowDirectedGraphService.getDirectedGraph(workflowId, v))
        .orElse(this.workflowDirectedGraphService.getDirectedGraph(workflowId));

    if (directGraph == null) {
      throw new NotFoundException(
          String.format("No workflow with id '%s' and version '%d' is found", workflowId, version));
    }
    return directGraph;
  }

  private static NodeView buildNode(Map<String, WorkflowNode> dictionary, String key,
      WorkflowNode workflowNode, NodeChildren children, List<String> parents) {
    NodeView.NodeViewBuilder nodeBuilder = NodeView.builder()
        .nodeId(key)
        .parents(parents)
        .children(children.getChildren()
            .stream()
            .map(c -> NodeView.ChildView.of(c, determineCondition(dictionary.get(c), key)))
            .collect(Collectors.toList()));

    String name = WorkflowNodeTypeHelper.toUpperUnderscore(workflowNode.getWrappedType().getSimpleName());
    nodeBuilder.type(WorkflowNodeTypeHelper.toType(name));
    nodeBuilder.group(WorkflowNodeTypeHelper.toGroup(name));
    return nodeBuilder.build();
  }

  @Nullable
  private static String determineCondition(WorkflowNode child, String key) {
    String condition = child.getIfCondition(key);
    if (condition == null) {
      condition = child.getElementType() == WorkflowNodeType.ACTIVITY_EXPIRED_EVENT ? "expired"
          : child.getElementType() == WorkflowNodeType.ACTIVITY_FAILED_EVENT ? "failed" : null;
    }
    return condition;
  }

  public List<VariableView> listWorkflowInstanceGlobalVars(String workflowId, String instanceId, Instant updatedBefore,
      Instant updatedAfter) {
    // check if the instance belongs to the provided workflow
    this.checkIsInstanceOfWorkflow(workflowId, instanceId);

    return variableQueryRepository.findGlobalVarsHistoryByWorkflowInstId(instanceId, updatedBefore, updatedAfter)
        .stream()
        .map(VariableView::new)
        .collect(Collectors.toList());
  }

  private WorkflowInstView checkIsInstanceOfWorkflow(String workflowId, String instanceId) {
    Optional<WorkflowInstView> instance = this.listWorkflowInstances(workflowId, null, null)
        .stream()
        .filter(workflowInstView -> workflowInstView.getInstanceId().equals(instanceId) && workflowInstView.getId()
            .equals(workflowId))
        .findAny();

    if (instance.isEmpty()) {
      throw new NotFoundException(
          String.format("Either no workflow deployed with id %s, or %s is not an instance of it", workflowId,
              instanceId));
    }
    return instance.get();
  }
}
