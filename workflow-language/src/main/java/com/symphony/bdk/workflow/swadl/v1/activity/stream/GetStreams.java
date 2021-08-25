package com.symphony.bdk.workflow.swadl.v1.activity.stream;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#list-streams-for-enterprise-v2">Get streams API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetStreams extends BaseActivity {
  private List<String> types;
  private String scope;
  private String origin;
  private String privacy;
  private String status;
  private String startDate;
  private String endDate;
  private String limit;
  private String skip;

  @JsonIgnore
  public Integer getLimitAsInt() {
    return toInt(limit);
  }

  @JsonIgnore
  public Integer getSkipAsInt() {
    return toInt(skip);
  }
}
