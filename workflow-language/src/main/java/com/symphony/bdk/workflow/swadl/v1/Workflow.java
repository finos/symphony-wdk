package com.symphony.bdk.workflow.swadl.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
  private Properties properties;

  @JsonProperty
  private Map<String, Object> variables = Collections.emptyMap();

  @JsonProperty
  private List<Activity> activities;

  @JsonIgnore
  private Long version;

  public Optional<Activity> getFirstActivity() {
    return activities.stream().findFirst();
  }

  public Properties getProperties() {
    return properties == null ? new Properties() : properties;
  }

  public boolean isToPublish() {
    return getProperties().getPublish();
  }
}
