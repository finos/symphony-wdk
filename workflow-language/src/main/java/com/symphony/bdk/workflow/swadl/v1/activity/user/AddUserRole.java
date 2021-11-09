package com.symphony.bdk.workflow.swadl.v1.activity.user;

import com.symphony.bdk.workflow.swadl.v1.Variable;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#add-role">Add role API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AddUserRole extends BaseActivity {
  private Variable<List<Variable<Number>>> userIds = Variable.value(List.of());
  private Variable<List<Variable<String>>> roles = Variable.value(List.of());
}
