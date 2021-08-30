package com.symphony.bdk.workflow.swadl.v1.activity.room;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#update-room-v3">Update room API</a>
 * @see <a href="https://developers.symphony.com/restapi/reference#de-or-re-activate-room">Activate room API</a>
 *
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UpdateRoom extends BaseActivity {
  private String streamId;

  private String roomName;
  private String roomDescription;
  private Map<String, String> keywords;

  private String membersCanInvite;
  private String discoverable;
  @JsonProperty("public")
  private String isPublic;
  private String readOnly;
  private String copyProtected;
  private String crossPod;
  private String viewHistory;
  private String multiLateralRoom;

  private String active;

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
  public Boolean getMultiLateralRoomAsBool() {
    return toBoolean(multiLateralRoom);
  }

  @JsonIgnore
  public Boolean getActiveAsBool() {
    return toBoolean(active);
  }
}
