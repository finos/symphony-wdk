package com.symphony.bdk.workflow.swadl.v1.activity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
public class CreateRoom extends BaseActivity {
  private String roomDescription;
  private List<String> uids;

  @JsonProperty("public")
  private boolean isPublic;

  // to support the usage of variables
  @JsonIgnore
  public List<Long> getUuidsAsLongs() {
    return uids.stream().map(Long::parseLong).collect(Collectors.toList());
  }
}

