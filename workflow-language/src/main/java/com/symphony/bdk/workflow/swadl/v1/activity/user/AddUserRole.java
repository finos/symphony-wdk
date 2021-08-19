package com.symphony.bdk.workflow.swadl.v1.activity.user;

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
  private List<String> userIds;
  private List<String> roles;
}
