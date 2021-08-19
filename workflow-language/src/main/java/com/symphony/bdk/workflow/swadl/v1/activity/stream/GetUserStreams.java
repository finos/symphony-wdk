package com.symphony.bdk.workflow.swadl.v1.activity.stream;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#list-user-streams">Get user streams API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetUserStreams extends BaseActivity {
  private List<String> types;
  private String includeInactiveStreams;
  private String limit;
  private String skip;

  public Boolean getIncludeInactiveStreams() {
    return toBoolean(includeInactiveStreams);
  }

  public Integer getLimit() {
    return toInt(limit);
  }

  public Integer getSkip() {
    return toInt(skip);
  }
}
