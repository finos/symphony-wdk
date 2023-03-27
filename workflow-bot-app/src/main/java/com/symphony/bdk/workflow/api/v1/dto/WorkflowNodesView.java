package com.symphony.bdk.workflow.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowNodesView {
  private String workflowId;
  private Long version;
  private Map<String, Object> variables;
  private List<NodeView> flowNodes;
}
