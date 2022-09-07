package com.symphony.bdk.workflow.engine.camunda.monitor.service;

import com.symphony.bdk.workflow.api.v1.dto.ActivitiesInstancesView;
import com.symphony.bdk.workflow.api.v1.dto.ActivityDefinitionView;
import com.symphony.bdk.workflow.api.v1.dto.ActivityInstanceView;
import com.symphony.bdk.workflow.api.v1.dto.EventDefinitionView;
import com.symphony.bdk.workflow.api.v1.dto.InstanceView;
import com.symphony.bdk.workflow.api.v1.dto.TaskDefinitionView;
import com.symphony.bdk.workflow.api.v1.dto.TaskTypeEnum;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowDefinitionVIew;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;
import com.symphony.bdk.workflow.api.v1.mapper.ActivitiesInstanceViewMapper;
import com.symphony.bdk.workflow.api.v1.mapper.WorkflowInstanceViewMapper;
import com.symphony.bdk.workflow.api.v1.mapper.WorkflowViewMapper;
import com.symphony.bdk.workflow.engine.WorkflowDirectGraph;
import com.symphony.bdk.workflow.engine.WorkflowNode;
import com.symphony.bdk.workflow.engine.camunda.WorkflowDirectGraphCachingService;
import com.symphony.bdk.workflow.engine.camunda.monitor.repository.ActivityInstanceMonitoringJavaApiRepository;
import com.symphony.bdk.workflow.engine.camunda.monitor.repository.WorkflowInstanceMonitoringJavaApiRepository;
import com.symphony.bdk.workflow.engine.camunda.monitor.repository.WorkflowMonitoringJavaApiRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class MonitoringService {
  private final WorkflowDirectGraphCachingService workflowDirectGraphCachingService;
  private final WorkflowMonitoringJavaApiRepository workflowMonitoringJavaApiRepository;
  private final WorkflowInstanceMonitoringJavaApiRepository workflowInstanceMonitoringJavaApiRepository;
  private final ActivityInstanceMonitoringJavaApiRepository activityInstanceMonitoringJavaApiRepository;

  public List<WorkflowView> listAllWorkflows() {
    return workflowMonitoringJavaApiRepository.listAllWorkflows()
        .stream()
        .map(WorkflowViewMapper::toWorkflowView)
        .distinct() // Workflows with different versions are returned as one item
        .collect(Collectors.toList());
  }

  public List<InstanceView> listWorkflowInstances(String workflowId) {
    return workflowInstanceMonitoringJavaApiRepository.listWorkflowInstances(workflowId)
        .stream()
        .map(WorkflowInstanceViewMapper::toWorkflowInstanceView)
        .collect(Collectors.toList());
  }

  public ActivitiesInstancesView listInstanceActivities(String workflowId, String instanceId) {
    ActivitiesInstancesView activitiesInstances = ActivitiesInstanceViewMapper.toActivitiesView(
        this.activityInstanceMonitoringJavaApiRepository.listInstanceActivities(instanceId), workflowId);

    // set activity type
    Map<String, String> activityIdToTypeMap = new HashMap<>();
    WorkflowDirectGraph directGraph = this.workflowDirectGraphCachingService.getDirectGraph(workflowId);
    directGraph.getDictionary().forEach((key, value) -> {
      WorkflowNode workflowNode = directGraph.getDictionary().get(key);
      if (workflowNode.getActivity() != null) {
        activityIdToTypeMap.put(key, workflowNode.getActivity().getClass().getSimpleName());
      }
    });

    activitiesInstances.getActivities()
        .forEach(activity -> {
          // set activity type
          activity.setType(TaskTypeEnum.findByAbbr(activityIdToTypeMap.get(activity.getActivityId()))); //TODO: try with unknown type, NP?

          // set previous activity (from execution not from swadl)
          String previousActivityId = getPreviousActivityInstance(
              activitiesInstances.getActivities()
                  .stream()
                  .map(ActivityInstanceView::getActivityId)
                  .collect(Collectors.toList()), activity.getActivityId());

          activity.setPreviousActivityId(previousActivityId);

          // set activity children
          activity.setNextActivityIds(directGraph.getChildren(activity.getActivityId()).getChildren());
        });

    return activitiesInstances;
  }

  private String getPreviousActivityInstance(List<String> historicActivityIds,
      String activityId) {
    int index = historicActivityIds.indexOf(activityId);

    if (index == -1 || index == 0) {
      return null;
    }

    return historicActivityIds.get(index - 1);
  }

  //TODO: handle case where the workflow is not deployed (404?)
  public WorkflowDefinitionVIew listWorkflowActivities(String workflowId) {
    WorkflowDefinitionVIew.WorkflowDefinitionVIewBuilder builder = WorkflowDefinitionVIew.builder()
        .workflowId(workflowId)
        .activities(new ArrayList<>())
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

      builder.activities(activities);
    }

    return builder.build();
  }
}
