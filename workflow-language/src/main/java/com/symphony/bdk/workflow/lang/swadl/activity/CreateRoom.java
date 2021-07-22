package com.symphony.bdk.workflow.lang.swadl.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class CreateRoom extends BaseActivity {
  private String roomDescription;
  private List<Long> uids;

  @JsonProperty("public")
  private boolean isPublic;
}

