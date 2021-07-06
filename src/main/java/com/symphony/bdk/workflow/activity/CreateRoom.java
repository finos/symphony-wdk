package com.symphony.bdk.workflow.activity;

import com.symphony.bdk.workflow.swadl.Event;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class CreateRoom {

  @JsonProperty("id")
  private String id;

  @JsonProperty("description")
  private String description;

  @JsonProperty("name")
  private String name;

  @JsonProperty("uids")
  private List<Long> uids;

  @JsonProperty("on")
  private Event on;

  @JsonProperty("public")
  private boolean isPublic;
}



