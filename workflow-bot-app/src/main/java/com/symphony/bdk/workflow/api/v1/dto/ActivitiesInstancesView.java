package com.symphony.bdk.workflow.api.v1.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ActivitiesInstancesView {
  private String workflowId;
  private List<ActivityInstanceView> activities;
}
