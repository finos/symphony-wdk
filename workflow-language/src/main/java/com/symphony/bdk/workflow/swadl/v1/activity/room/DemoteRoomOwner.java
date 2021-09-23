package com.symphony.bdk.workflow.swadl.v1.activity.room;

import com.symphony.bdk.workflow.swadl.v1.Variable;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

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
  private Variable<List<Number>> userIds = Variable.value(List.of());
}
