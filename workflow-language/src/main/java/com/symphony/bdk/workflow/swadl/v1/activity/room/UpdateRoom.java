package com.symphony.bdk.workflow.swadl.v1.activity.room;

import com.symphony.bdk.workflow.swadl.v1.Variable;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

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
public class UpdateRoom extends BaseActivity {
  private String streamId;

  @Nullable private String roomName;
  @Nullable private String roomDescription;
  @Nullable private Variable<Map<String, String>> keywords;

  private Variable<Boolean> membersCanInvite = Variable.nullValue();
  private Variable<Boolean> discoverable = Variable.nullValue();

  @JsonProperty("public")
  private Variable<Boolean> isPublic = Variable.nullValue();

  private Variable<Boolean> readOnly = Variable.nullValue();
  private Variable<Boolean> copyProtected = Variable.nullValue();
  private Variable<Boolean> crossPod = Variable.nullValue();
  private Variable<Boolean> viewHistory = Variable.nullValue();
  private Variable<Boolean> multilateralRoom = Variable.nullValue();

  @Nullable private Variable<Boolean> active;
}
