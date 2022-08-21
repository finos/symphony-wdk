package com.symphony.bdk.workflow.swadl.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Workflow {
  @JsonProperty
  private String id;

  @JsonProperty
  private Metadata metadata;

  @JsonProperty
  private Map<String, Object> variables = Collections.emptyMap();

  @JsonProperty
  private List<Activity> activities;

  public Optional<Activity> getFirstActivity() {
    return activities.stream().findFirst();
  }

  public Metadata getMetadata() {
    return metadata == null ? new Metadata() : metadata;
  }

  public boolean isToPublish() {
    return getMetadata().getPublish();
  }
}
