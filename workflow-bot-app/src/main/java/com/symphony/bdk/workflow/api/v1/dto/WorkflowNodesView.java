package com.symphony.bdk.workflow.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowNodesView {
  private List<NodeView> nodes;
  private VariableView globalVariables;
  private Map<String, Object> error;
}
