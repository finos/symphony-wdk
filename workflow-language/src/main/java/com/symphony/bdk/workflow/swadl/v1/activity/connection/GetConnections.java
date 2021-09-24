package com.symphony.bdk.workflow.swadl.v1.activity.connection;

import com.symphony.bdk.workflow.swadl.v1.Variable;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#list-connections">List Connections API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetConnections extends BaseActivity {
  private Variable<List<Number>> userIds = Variable.value(List.of());
  private String status;
}
