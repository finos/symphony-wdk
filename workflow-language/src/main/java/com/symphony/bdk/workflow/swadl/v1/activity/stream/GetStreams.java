package com.symphony.bdk.workflow.swadl.v1.activity.stream;

import com.symphony.bdk.workflow.swadl.v1.Variable;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import javax.annotation.Nullable;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#list-streams-for-enterprise-v2">Get streams API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetStreams extends BaseActivity {
  @Nullable private Variable<List<String>> types;
  private String scope;
  private String origin;
  private String privacy;
  private String status;
  private String startDate;
  private String endDate;
  private Variable<Number> limit;
  private Variable<Number> skip;

}
