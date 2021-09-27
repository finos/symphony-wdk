package com.symphony.bdk.workflow.swadl.v1.activity.message;

import com.symphony.bdk.workflow.swadl.v1.Variable;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

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
  private Variable<Number> skip = Variable.nullValue();
  private Variable<Number> limit = Variable.nullValue();
}
