package com.symphony.bdk.workflow.swadl.v1.activity.stream;

import com.symphony.bdk.workflow.swadl.v1.Variable;
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
  private Variable<List<Variable<String>>> types;
  private Variable<Boolean> includeInactiveStreams = Variable.nullValue();
  private Variable<Number> limit;
  private Variable<Number> skip;

}
