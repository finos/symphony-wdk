package com.symphony.bdk.workflow.lang.swadl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Activity {

  @JsonProperty("id")
  private String id;

  @JsonProperty("description")
  private String description;

  @JsonProperty("on")
  private Event on;

  private String reply;
}
