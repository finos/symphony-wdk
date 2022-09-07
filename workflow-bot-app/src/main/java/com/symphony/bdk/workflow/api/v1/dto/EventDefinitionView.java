package com.symphony.bdk.workflow.api.v1.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class EventDefinitionView extends TaskDefinitionView {

  public EventDefinitionView(TaskDefinitionView taskDefinitionView) {
    super(taskDefinitionView.getType(), taskDefinitionView.getParents(), taskDefinitionView.getChildren());
  }
}
