package com.symphony.bdk.workflow.api.v1.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class ActivityDefinitionView extends TaskDefinitionView {
  private String activityId;

  public ActivityDefinitionView(TaskDefinitionView taskDefinitionView, String activityId) {
    super(taskDefinitionView.getType(), taskDefinitionView.getParents(), taskDefinitionView.getChildren());
    this.activityId = activityId;
  }
}
