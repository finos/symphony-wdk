package com.symphony.bdk.workflow.api.v1.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ActivitiesView {
  private List<ActivityView> activities;
}
