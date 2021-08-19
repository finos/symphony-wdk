package com.symphony.bdk.workflow.swadl.v1.activity.stream;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#stream-members">Get stream members API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetStreamMembers extends BaseActivity {
  private String streamId;
  private String limit;
  private String skip;

  public Integer getLimit() {
    return toInt(limit);
  }

  public Integer getSkip() {
    return toInt(skip);
  }
}
