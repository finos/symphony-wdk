package com.symphony.bdk.workflow.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeView {
  private String workflowId;
  private String instanceId;
  private String nodeId;
  private String type;
  private String group;
  private Instant startDate;
  private Instant endDate;
  private Duration duration;
  private Map<String, Object> outputs;
}
