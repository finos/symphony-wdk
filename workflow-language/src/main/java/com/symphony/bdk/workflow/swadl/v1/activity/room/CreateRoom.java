package com.symphony.bdk.workflow.swadl.v1.activity.room;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#create-room-v3">Create room API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CreateRoom extends BaseActivity {
  private String roomName;
  private String roomDescription;
  private List<String> uids;

  @JsonProperty("public")
  private String isPublic;

  // to support the usage of variables
  @JsonIgnore
  public List<Long> getUuidsAsLongs() {
    if (uids == null) {
      return null;
    }
    return uids.stream().map(Long::parseLong).collect(Collectors.toList());
  }

  @JsonIgnore
  public Boolean isPublicAsBool() {
    return Boolean.valueOf(isPublic);
  }
}

