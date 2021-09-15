package com.symphony.bdk.workflow.swadl.v1.activity.room;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;
import javax.annotation.Nullable;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#update-room-v3">Update room API</a>
 * @see <a href="https://developers.symphony.com/restapi/reference#de-or-re-activate-room">Activate room API</a>
 *
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UpdateRoom extends BaseActivity {
  private String streamId;

  @Nullable private String roomName;
  @Nullable private String roomDescription;
  @Nullable private Map<String, String> keywords;

  @Nullable private String membersCanInvite;
  @Nullable private String discoverable;

  @JsonProperty("public")
  @Nullable private String isPublic;

  @Nullable private String readOnly;
  @Nullable private String copyProtected;
  @Nullable private String crossPod;
  @Nullable private String viewHistory;
  @Nullable private String multilateralRoom;

  @Nullable private String active;

  @JsonIgnore
  public Boolean getMembersCanInviteAsBool() {
    return toBoolean(membersCanInvite);
  }

  @JsonIgnore
  public Boolean getDiscoverableAsBool() {
    return toBoolean(discoverable);
  }

  @JsonIgnore
  public Boolean getIsPublicAsBool() {
    return toBoolean(isPublic);
  }

  @JsonIgnore
  public Boolean getReadOnlyAsBool() {
    return toBoolean(readOnly);
  }

  @JsonIgnore
  public Boolean getCopyProtectedAsBool() {
    return toBoolean(copyProtected);
  }

  @JsonIgnore
  public Boolean getCrossPodAsBool() {
    return toBoolean(crossPod);
  }

  @JsonIgnore
  public Boolean getViewHistoryAsBool() {
    return toBoolean(viewHistory);
  }

  @JsonIgnore
  public Boolean getMultilateralRoomAsBool() {
    return toBoolean(multilateralRoom);
  }

  @JsonIgnore
  public Boolean getActiveAsBool() {
    return toBoolean(active);
  }
}
