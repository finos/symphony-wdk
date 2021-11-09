package com.symphony.bdk.workflow.swadl.v1.activity.user;

import com.symphony.bdk.workflow.swadl.v1.Variable;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import javax.annotation.Nullable;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#users-lookup-v3">Search users API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetUsers extends BaseActivity {

  @Nullable
  private Variable<List<Variable<Number>>> userIds;
  @Nullable
  private Variable<List<Variable<String>>> emails;
  @Nullable
  private Variable<List<Variable<String>>> usernames;

  private Variable<Boolean> local = Variable.nullValue();
  private Variable<Boolean> active = Variable.nullValue();

}
