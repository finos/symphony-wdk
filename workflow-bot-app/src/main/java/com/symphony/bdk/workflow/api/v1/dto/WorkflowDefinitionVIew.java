package com.symphony.bdk.workflow.api.v1.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class WorkflowDefinitionVIew {
  private String workflowId;
  private List<Map<String, String>> variables;
  private List<TaskDefinitionView> flowNodes;
}
