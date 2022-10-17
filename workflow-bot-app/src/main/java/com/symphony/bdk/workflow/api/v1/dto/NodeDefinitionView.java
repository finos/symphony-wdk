package com.symphony.bdk.workflow.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class NodeDefinitionView {
  private String nodeId;
  private String type;
  private String group;
  private List<String> parents;
  private List<ChildView> children;

  @AllArgsConstructor(staticName = "of")
  @NoArgsConstructor
  @Data
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ChildView {
    private String nodeId;
    private String condition;

    public ChildView(String nodeId) {
      this.nodeId = nodeId;
    }

    public static ChildView of(String nodeId) {
      return new ChildView(nodeId);
    }
  }
}
