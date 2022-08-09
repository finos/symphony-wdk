package com.symphony.bdk.workflow.swadl.v1.activity.room;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;
import com.symphony.bdk.workflow.swadl.v1.activity.Obo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#demote-owner">Demote owner API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DemoteRoomOwner extends BaseActivity {
  private String streamId;
  private List<Long> userIds = List.of();
  private Obo obo;
}
