package com.symphony.bdk.workflow.api.v1.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ActivityView {
  private String workflowId;
  private String instanceId;
  private String activityId;
  private String type; //TODO: enum needed here
  private InstanceStatusEnum status;
  private Long startDate;
  private Long endDate;

  private String previousActivityId; //TODO: ignore for now
  private List<String> nextActivityIds; //TODO: ignore for now
  private List<String> variablesModified; //TODO: ignore for now
}
