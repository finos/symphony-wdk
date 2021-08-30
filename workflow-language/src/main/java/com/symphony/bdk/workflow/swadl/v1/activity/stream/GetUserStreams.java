package com.symphony.bdk.workflow.swadl.v1.activity.stream;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

  @JsonIgnore
  public Boolean getIncludeInactiveStreamsAsBool() {
    return toBoolean(includeInactiveStreams);
  }

  @JsonIgnore
  public Integer getLimitAsInt() {
    return toInt(limit);
  }

  @JsonIgnore
  public Integer getSkipAsInt() {
    return toInt(skip);
  }
}
