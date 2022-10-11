package com.symphony.bdk.workflow.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class TaskDefinitionView {
  private String nodeId;
  private String type;
  private String group;
  private List<String> parents;
  private List<String> children;
}
