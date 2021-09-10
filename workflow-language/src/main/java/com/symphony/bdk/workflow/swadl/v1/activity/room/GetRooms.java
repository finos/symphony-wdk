package com.symphony.bdk.workflow.swadl.v1.activity.room;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import javax.annotation.Nullable;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#search-rooms-v3">Get rooms API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetRooms extends BaseActivity {
  private String query;
  @Nullable private List<String> labels;
  @Nullable private String active;

  @JsonProperty("private")
  @Nullable private String isPrivate;

  @Nullable private String creatorId;
  @Nullable private String ownerId;
  @Nullable private String memberId;

  @Nullable private String sortOrder;

  @Nullable private String limit;
  @Nullable private String skip;

  @JsonIgnore
  public Boolean getActiveAsBool() {
    return toBoolean(active);
  }

  @JsonIgnore
  public Boolean getIsPrivateAsBool() {
    return toBoolean(isPrivate);
  }

  @JsonIgnore
  public Integer getLimitAsInt() {
    return toInt(limit);
  }

  @JsonIgnore
  public Integer getSkipAsInt() {
    return toInt(skip);
  }

}
