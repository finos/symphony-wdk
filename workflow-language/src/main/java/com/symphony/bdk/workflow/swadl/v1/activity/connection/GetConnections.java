package com.symphony.bdk.workflow.swadl.v1.activity.connection;

import com.symphony.bdk.workflow.swadl.v1.Variable;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import javax.annotation.Nullable;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#list-connections">List Connections API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetConnections extends BaseActivity {
  private Variable<List<Variable<Number>>> userIds = Variable.nullValue();
  @Nullable private String status;
}
