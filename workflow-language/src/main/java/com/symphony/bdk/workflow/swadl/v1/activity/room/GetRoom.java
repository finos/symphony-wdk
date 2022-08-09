package com.symphony.bdk.workflow.swadl.v1.activity.room;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;
import com.symphony.bdk.workflow.swadl.v1.activity.Obo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#room-info-v3">Get room API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetRoom extends BaseActivity {
  private String streamId;
  private Obo obo;
}
