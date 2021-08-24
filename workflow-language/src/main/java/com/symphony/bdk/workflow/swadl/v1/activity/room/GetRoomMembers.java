package com.symphony.bdk.workflow.swadl.v1.activity.room;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#room-members">Get room members API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetRoomMembers extends BaseActivity {
  private String streamId;
}
