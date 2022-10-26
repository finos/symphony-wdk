package com.symphony.bdk.workflow.monitoring.service;

import static com.symphony.bdk.workflow.api.v1.dto.TaskTypeEnum.GATEWAY;
import static com.symphony.bdk.workflow.api.v1.dto.TaskTypeEnum.GATEWAY_JOIN;
import static com.symphony.bdk.workflow.api.v1.dto.TaskTypeEnum.findByAbbr;
import static com.symphony.bdk.workflow.engine.WorkflowDirectGraph.NodeChildren;

import com.symphony.bdk.workflow.api.v1.dto.ActivityInstanceView;
import com.symphony.bdk.workflow.api.v1.dto.NodeDefinitionView;
import com.symphony.bdk.workflow.api.v1.dto.StatusEnum;
import com.symphony.bdk.workflow.api.v1.dto.TaskTypeEnum;
import com.symphony.bdk.workflow.api.v1.dto.VariableView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowActivitiesView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowDefinitionView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstLifeCycleFilter;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;
import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.engine.WorkflowDirectGraph;
import com.symphony.bdk.workflow.engine.WorkflowNode;
import com.symphony.bdk.workflow.engine.WorkflowNodeType;
import com.symphony.bdk.workflow.engine.camunda.WorkflowDirectGraphCachingService;
import com.symphony.bdk.workflow.engine.executor.ActivityExecutorContext;
import com.symphony.bdk.workflow.monitoring.repository.ActivityQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.VariableQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.WorkflowInstQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.WorkflowQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.domain.ActivityInstanceDomain;
import com.symphony.bdk.workflow.monitoring.repository.domain.VariablesDomain;
import com.symphony.bdk.workflow.monitoring.repository.domain.WorkflowInstanceDomain;

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
  private final WorkflowDirectGraphCachingService workflowDirectGraphCachingService;
  private final WorkflowQueryRepository workflowQueryRepository;
  private final WorkflowInstQueryRepository workflowInstQueryRepository;
  private final ActivityQueryRepository activityQueryRepository;
  private final VariableQueryRepository variableQueryRepository;
  private final ObjectConverter objectConverter;

  private static final String GATEWAY_SUFFIX = "_gateway";

  public List<WorkflowView> listAllWorkflows() {
    return objectConverter.convertCollection(workflowQueryRepository.findAll(), WorkflowView.class);
  }

  public List<WorkflowInstView> listWorkflowInstances(String workflowId, String status) {
    List<WorkflowInstanceDomain> allById;

    if (status != null) {
      allById = workflowInstQueryRepository.findAllById(workflowId, StatusEnum.toInstanceStatusEnum(status));
    } else {
      allById = workflowInstQueryRepository.findAllById(workflowId);
    }

    return objectConverter.convertCollection(allById, WorkflowInstView.class);
  }

  public WorkflowActivitiesView listWorkflowInstanceActivities(String workflowId, String instanceId,
      WorkflowInstLifeCycleFilter lifeCycleFilter) {

    // check if the instance belongs to the provided workflow
    this.checkIsInstanceOfWorkflow(workflowId, instanceId);

    List<ActivityInstanceDomain> activityInstances =
        activityQueryRepository.findAllByWorkflowInstanceId(workflowId, instanceId, lifeCycleFilter);
    List<ActivityInstanceView> activities =
        objectConverter.convertCollection(activityInstances, ActivityInstanceView.class);

    // set activity type
    WorkflowDirectGraph directGraph = this.workflowDirectGraphCachingService.getDirectGraph(workflowId);

    if (directGraph != null) {
      Map<String, String> activityIdToTypeMap = directGraph.getDictionary().entrySet()
          .stream().filter(e -> directGraph.getDictionary().get(e.getKey()).getActivity() != null)
          .collect(Collectors.toMap(
              Map.Entry::getKey,
              e -> directGraph.getDictionary().get(e.getKey()).getActivity().getClass().getSimpleName())
          );

      // set activity type
      activities.forEach(
          activity -> activity.setType(findByAbbr(activityIdToTypeMap.get(activity.getActivityId()))));
    }

    VariablesDomain globalVariables = this.variableQueryRepository.findVarsByWorkflowInstanceIdAndVarName(instanceId,
        ActivityExecutorContext.VARIABLES);
    VariablesDomain error =
        this.variableQueryRepository.findVarsByWorkflowInstanceIdAndVarName(instanceId, ActivityExecutorContext.ERROR);
    WorkflowActivitiesView result = new WorkflowActivitiesView();
    result.setActivities(activities);
    result.setGlobalVariables(new VariableView(globalVariables));
    if (!error.getOutputs().isEmpty()) {
      result.setError(error.getOutputs());
    }
    return result;
  }

  public WorkflowDefinitionView getWorkflowDefinition(String workflowId) {
    WorkflowDirectGraph directGraph = readWorkflowDirectedGraph(workflowId);
    List<NodeDefinitionView> activities = new ArrayList<>();
    Map<String, WorkflowNode> dictionary = directGraph.getDictionary();
    dictionary.forEach((key, value) -> {
      WorkflowNode workflowNode = dictionary.get(key);
      NodeChildren children = directGraph.getChildren(key);
      List<String> parents = directGraph.getParents(key);
      NodeDefinitionView node = buildNode(directGraph, activities, dictionary, key, workflowNode, children, parents);
      activities.add(node);
    });
    return WorkflowDefinitionView.builder()
        .workflowId(workflowId)
        .flowNodes(activities)
        .variables(directGraph.getVariables()).build();
  }

  private WorkflowDirectGraph readWorkflowDirectedGraph(String workflowId) {
    WorkflowDirectGraph directGraph = this.workflowDirectGraphCachingService.getDirectGraph(workflowId);
    if (directGraph == null) {
      throw new IllegalArgumentException(
          String.format("No workflow deployed with id '%s' is found", workflowId));
    }
    return directGraph;
  }

  private static NodeDefinitionView buildNode(WorkflowDirectGraph directGraph, List<NodeDefinitionView> activities,
      Map<String, WorkflowNode> dictionary, String key, WorkflowNode workflowNode, NodeChildren children,
      List<String> parents) {
    NodeDefinitionView.NodeDefinitionViewBuilder nodeBuilder = NodeDefinitionView.builder()
        .nodeId(key)
        .parents(parents)
        .children(children.getChildren()
            .stream()
            .map(c -> NodeDefinitionView.ChildView.of(c, determineCondition(dictionary.get(c), key)))
            .collect(Collectors.toList()));

    if (workflowNode.getActivity() != null) {
      TaskTypeEnum taskTypeEnum = findByAbbr(workflowNode.getActivity().getClass().getSimpleName());
      nodeBuilder.type(taskTypeEnum.toType());
      nodeBuilder.group(taskTypeEnum.toGroup());
    } else if (workflowNode.getEvent() != null) {
      TaskTypeEnum taskTypeEnum = findByAbbr(workflowNode.getEvent().getEventType());
      nodeBuilder.type(taskTypeEnum.toType());
      nodeBuilder.group(taskTypeEnum.toGroup());
    } else if (workflowNode.getElementType() == WorkflowNodeType.JOIN_ACTIVITY) {
      nodeBuilder.type(GATEWAY_JOIN);
      nodeBuilder.group(GATEWAY);
    }
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

  private void checkIsInstanceOfWorkflow(String workflowId, String instanceId) {
    Optional<WorkflowInstView> instance = this.listWorkflowInstances(workflowId, null)
        .stream()
        .filter(workflowInstView -> workflowInstView.getInstanceId().equals(instanceId) && workflowInstView.getId()
            .equals(workflowId))
        .findAny();

    if (instance.isEmpty()) {
      throw new IllegalArgumentException(
          String.format("Either no workflow deployed with id %s, or %s is not an instance of it", workflowId,
              instanceId));
    }
  }
}
