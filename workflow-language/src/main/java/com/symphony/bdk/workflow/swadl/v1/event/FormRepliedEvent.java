package com.symphony.bdk.workflow.swadl.v1.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FormRepliedEvent extends InnerEvent {
  @JsonProperty
  private String formId;

  @JsonProperty
  private Boolean exclusive = true;
}
