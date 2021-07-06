package com.symphony.bdk.workflow.swadl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Optional;

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
  private List<Activity> activities;

  public Optional<Activity> getFirstActivity() {
    if (activities.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(activities.get(0));
  }
}

