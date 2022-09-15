package com.symphony.bdk.workflow.monitoring.service;

import com.symphony.bdk.workflow.api.v1.dto.ActivityDefinitionView;
import com.symphony.bdk.workflow.api.v1.dto.ActivityInstanceView;
import com.symphony.bdk.workflow.api.v1.dto.EventDefinitionView;
import com.symphony.bdk.workflow.api.v1.dto.TaskDefinitionView;
import com.symphony.bdk.workflow.api.v1.dto.TaskTypeEnum;
import com.symphony.bdk.workflow.api.v1.dto.VariableView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowActivitiesView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowDefinitionView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;
import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.engine.WorkflowDirectGraph;
import com.symphony.bdk.workflow.engine.WorkflowNode;
import com.symphony.bdk.workflow.engine.camunda.WorkflowDirectGraphCachingService;
import com.symphony.bdk.workflow.monitoring.repository.ActivityQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.VariableQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.WorkflowInstQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.WorkflowQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.domain.VariablesDomain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class MonitoringService {
  private final WorkflowDirectGraphCachingService workflowDirectGraphCachingService;
  private final WorkflowQueryRepository workflowQueryRepository;
  private final WorkflowInstQueryRepository workflowInstQueryRepository;
  private final ActivityQueryRepository activityQueryRepository;
  private final VariableQueryRepository variableQueryRepository;
  private final ObjectConverter objectConverter;

  public List<WorkflowView> listAllWorkflows() {
    return objectConverter.convertCollection(workflowQueryRepository.findAll(), WorkflowView.class);
  }

  public List<WorkflowInstView> listWorkflowInstances(String workflowId) {
    return objectConverter.convertCollection(workflowInstQueryRepository.findAllById(workflowId),
        WorkflowInstView.class);
  }

  public WorkflowActivitiesView listWorkflowInstanceActivities(String workflowId, String instanceId) {
    List<ActivityInstanceView> activities =
        objectConverter.convertCollection(activityQueryRepository.findAllByWorkflowInstanceId(instanceId),
            ActivityInstanceView.class);

    // set activity type
    WorkflowDirectGraph directGraph = this.workflowDirectGraphCachingService.getDirectGraph(workflowId);
    if (directGraph != null) {
      Map<String, String> activityIdToTypeMap = directGraph.getDictionary().entrySet()
          .stream().filter(e -> directGraph.getDictionary().get(e.getKey()).getActivity() != null)
          .collect(Collectors.toMap(
              Map.Entry::getKey,
              e -> directGraph.getDictionary().get(e.getKey()).getActivity().getClass().getSimpleName())
          );

      activities
          .forEach(activity -> {
            // set activity type
            activity.setType(TaskTypeEnum.findByAbbr(
                activityIdToTypeMap.get(activity.getActivityId())));
          });
    }

    VariablesDomain globalVariables = this.variableQueryRepository.findGlobalByWorkflowInstanceId(instanceId);
    WorkflowActivitiesView result = new WorkflowActivitiesView();
    result.setActivities(activities);
    result.setGlobalVariables(new VariableView(globalVariables));
    return result;
  }

  //TODO: handle case where the workflow is not deployed (404?)
  public WorkflowDefinitionView getWorkflowDefinition(String workflowId) {
    WorkflowDefinitionView.WorkflowDefinitionViewBuilder builder = WorkflowDefinitionView.builder()
        .workflowId(workflowId)
        .flowNodes(new ArrayList<>())
        .variables(Collections.emptyList()); //TODO: handles variables

    WorkflowDirectGraph directGraph = this.workflowDirectGraphCachingService.getDirectGraph(workflowId);
    ArrayList<TaskDefinitionView> activities = new ArrayList<>();
    if (directGraph != null) {
      Map<String, WorkflowNode> dictionary = directGraph.getDictionary();
      dictionary.forEach((key, value) -> {
        WorkflowNode workflowNode = dictionary.get(key);

        TaskDefinitionView.TaskDefinitionViewBuilder taskDefinitionViewBuilder =
            TaskDefinitionView.builder()
                .parents(directGraph.getParents(workflowNode.getId()))
                .children(directGraph.getChildren(workflowNode.getId()).getChildren());

        if (workflowNode.getActivity() != null) {
          taskDefinitionViewBuilder.type(
              TaskTypeEnum.findByAbbr(workflowNode.getActivity().getClass().getSimpleName()));

          ActivityDefinitionView activityDefinitionView =
              new ActivityDefinitionView(taskDefinitionViewBuilder.build(), workflowNode.getActivity().getId());

          activities.add(activityDefinitionView);
        } else if (workflowNode.getEvent() != null) {
          taskDefinitionViewBuilder.type(
              TaskTypeEnum.findByAbbr(workflowNode.getEvent().getEventType()));

          EventDefinitionView eventDefinitionView = new EventDefinitionView(taskDefinitionViewBuilder.build());
          activities.add(eventDefinitionView);
        }
      });

      builder.flowNodes(activities);
    }

    return builder.build();
  }
}
