package com.symphony.bdk.workflow.swadl.v1.activity.stream;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.annotation.Nullable;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#stream-members">Get stream members API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetStreamMembers extends BaseActivity {
  private String streamId;
  @Nullable private Integer limit;
  @Nullable private Integer skip;
}
