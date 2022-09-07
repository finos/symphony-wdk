package com.symphony.bdk.workflow.api.v1.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ActivityInstanceView {
  private String instanceId;
  private String activityId;
  private TaskTypeEnum type;
  private InstanceStatusEnum status;
  private String previousActivityId;
  private List<String> nextActivityIds;
  private Long startDate;
  private Long endDate;

  private List<String> variablesModified; //TODO: ignore for now
}
