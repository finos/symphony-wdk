package com.symphony.bdk.workflow.swadl.v1.activity.room;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

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
  private List<String> labels;
  private Boolean active;

  @JsonProperty("private")
  private Boolean isPrivate;

  @Nullable private String creatorId;
  @Nullable private String ownerId;
  @Nullable private String memberId;

  @Nullable private String sortOrder;

  @Nullable private Integer limit;
  @Nullable private Integer skip;

}
