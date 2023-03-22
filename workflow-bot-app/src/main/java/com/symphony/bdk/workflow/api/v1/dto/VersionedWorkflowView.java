package com.symphony.bdk.workflow.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VersionedWorkflowView {
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private String id;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private String workflowId;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Long version;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Boolean active;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Boolean published;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private String deploymentId;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Long createdBy;

  private String swadl;

  private String description;
}
