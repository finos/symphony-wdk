package com.symphony.bdk.workflow.lang.swadl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Workflow {
  private String name;
  private int version;
  private List<Variable> global;
  private List<Activity> activities;

  public Optional<Activity> getFirstActivity() {
    if (activities.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(activities.get(0));
  }
}
