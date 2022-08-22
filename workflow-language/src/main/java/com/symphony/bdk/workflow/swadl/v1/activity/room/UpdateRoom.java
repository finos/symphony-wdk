package com.symphony.bdk.workflow.swadl.v1.activity.room;

import com.symphony.bdk.workflow.swadl.v1.activity.OboActivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;
import javax.annotation.Nullable;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#update-room-v3">Update room API</a>
 * @see <a href="https://developers.symphony.com/restapi/reference#de-or-re-activate-room">Activate room API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UpdateRoom extends OboActivity {
  private String streamId;

  @Nullable private String roomName;
  @Nullable private String roomDescription;
  @Nullable private Map<String, String> keywords;

  private Boolean membersCanInvite;
  private Boolean discoverable;

  @JsonProperty("public")
  private Boolean isPublic;

  private Boolean readOnly;
  private Boolean copyProtected;
  private Boolean crossPod;
  private Boolean viewHistory;
  private Boolean multilateralRoom;

  @Nullable private Boolean active;
}
