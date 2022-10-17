package com.symphony.bdk.workflow.api.v1.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class WorkflowDefinitionView {
  private String workflowId;
  private Map<String, Object> variables;
  private List<NodeDefinitionView> flowNodes;
}
