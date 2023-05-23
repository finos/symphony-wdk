package com.symphony.bdk.workflow.api.v1.dto;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
@Builder
public class SwadlView {
  private String description;
  @NotEmpty(message = "SWADL content must not be empty.")
  private String swadl;
  private Long createdBy;
}
