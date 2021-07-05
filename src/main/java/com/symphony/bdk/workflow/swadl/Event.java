package com.symphony.bdk.workflow.swadl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.LinkedHashMap;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {
  @JsonProperty("message")
  private LinkedHashMap<String, String> content;

  @JsonProperty("user-joined")
  private LinkedHashMap<String, String> streamId;


}
