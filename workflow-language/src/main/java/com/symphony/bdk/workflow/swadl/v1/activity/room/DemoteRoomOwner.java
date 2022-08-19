package com.symphony.bdk.workflow.swadl.v1.activity.room;

import com.symphony.bdk.workflow.swadl.v1.activity.OboActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#demote-owner">Demote owner API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DemoteRoomOwner extends OboActivity {
  private String streamId;
  private List<Long> userIds = List.of();
}
