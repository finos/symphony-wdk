package com.symphony.bdk.workflow.swadl.v1.activity.room;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

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

  public Boolean getMembersCanInvite() {
    return toBoolean(membersCanInvite);
  }

  public Boolean getDiscoverable() {
    return toBoolean(discoverable);
  }

  public Boolean getIsPublic() {
    return toBoolean(isPublic);
  }

  public Boolean getReadOnly() {
    return toBoolean(readOnly);
  }

  public Boolean getCopyProtected() {
    return toBoolean(copyProtected);
  }

  public Boolean getCrossPod() {
    return toBoolean(crossPod);
  }

  public Boolean getViewHistory() {
    return toBoolean(viewHistory);
  }

  public Boolean getMultiLateralRoom() {
    return toBoolean(multiLateralRoom);
  }

  public Boolean getActive() {
    return toBoolean(active);
  }
}
