package com.symphony.bdk.workflow.swadl.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Properties {

  @JsonProperty
  private Boolean publish = true;

  @JsonProperty
  private String version = "";
}
