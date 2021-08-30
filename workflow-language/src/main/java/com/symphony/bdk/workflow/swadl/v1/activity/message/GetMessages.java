package com.symphony.bdk.workflow.swadl.v1.activity.message;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#messages-v4">Messages API</a>
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class GetMessages extends BaseActivity {
  private String streamId;
  private String since;
  private String skip;
  private String limit;

  @JsonIgnore
  public Integer getSkipAsInt() {
    return toInt(skip);
  }

  @JsonIgnore
  public Integer getLimitAsInt() {
    return toInt(limit);
  }
}
