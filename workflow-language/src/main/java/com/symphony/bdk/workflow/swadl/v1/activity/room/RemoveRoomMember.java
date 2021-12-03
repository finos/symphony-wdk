package com.symphony.bdk.workflow.swadl.v1.activity.room;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#remove-member">Remover member API</a>
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class RemoveRoomMember extends BaseActivity {
  private String streamId;
  private List<Long> userIds = List.of();
}
