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

  private Boolean membersCanInvite;
  private Boolean discoverable;
  @JsonProperty("public")
  private Boolean isPublic;
  private Boolean readOnly;
  private Boolean copyProtected;
  private Boolean crossPod;
  private Boolean viewHistory;
  private Boolean multiLateralRoom;

  private Boolean active;
}
