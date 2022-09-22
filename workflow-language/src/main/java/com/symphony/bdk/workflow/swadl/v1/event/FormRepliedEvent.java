package com.symphony.bdk.workflow.swadl.v1.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FormRepliedEvent {
  @JsonProperty
  private String formId;

  @JsonProperty
  private Boolean exclusive = true;
}
