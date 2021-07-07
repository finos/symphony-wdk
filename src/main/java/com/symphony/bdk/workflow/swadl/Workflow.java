package com.symphony.bdk.workflow.swadl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Workflow {
  @JsonProperty("name")
  private String name;

  @JsonProperty("version")
  private int version;

  @JsonProperty("global")
  private List<Variable> global;

  @JsonProperty("activities")
  private List<LinkedHashMap<String, Activity>> activities;
}
