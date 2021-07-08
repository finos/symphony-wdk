package com.symphony.bdk.workflow.lang.swadl.activity;

import com.symphony.bdk.workflow.engine.executor.CreateRoomExecutor;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CreateRoom extends BaseActivity<CreateRoomExecutor> {

  @JsonProperty("room-description")
  private String roomDescription;
  private List<Long> uids;

  @JsonProperty("public")
  private boolean isPublic;
}

