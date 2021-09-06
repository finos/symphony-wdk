package com.symphony.bdk.workflow.swadl.v1.activity.room;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collections;
import java.util.List;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#add-member">Add member API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AddRoomMember extends BaseActivity {
  private String streamId;
  private List<String> userIds = Collections.emptyList();
}
