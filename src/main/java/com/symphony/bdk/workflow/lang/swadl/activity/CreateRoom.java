package com.symphony.bdk.workflow.lang.swadl.activity;

import com.symphony.bdk.workflow.lang.swadl.Event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CreateRoom {

  private String id;
  private String description;

  @JsonProperty("room-description")
  private String roomDescription;
  private String name;
  private List<Long> uids;
  private Event on;

  @JsonProperty("public")
  private boolean isPublic;
}

