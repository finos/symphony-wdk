package com.symphony.bdk.workflow.api.v1.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder(toBuilder=true)
public class TaskDefinitionView {
  private TaskTypeEnum type;
  private List<String> parents;
  private List<String> children;
}
