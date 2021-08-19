package com.symphony.bdk.workflow.swadl.v1.activity.room;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#search-rooms-v3">Get rooms API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetRooms extends BaseActivity {
  private String query;
  private List<String> labels;
  private String active;
  @JsonProperty("private")
  private String isPrivate;

  private String creatorId;
  private String ownerId;
  private String memberId;

  private String sortOrder;

  private String limit;
  private String skip;

  public Boolean getActive() {
    return toBoolean(active);
  }

  public Boolean getIsPrivate() {
    return toBoolean(isPrivate);
  }

  public Integer getLimit() {
    return toInt(limit);
  }

  public Integer getSkip() {
    return toInt(skip);
  }
}
