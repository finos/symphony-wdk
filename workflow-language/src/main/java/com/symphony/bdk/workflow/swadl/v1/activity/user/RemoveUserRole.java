package com.symphony.bdk.workflow.swadl.v1.activity.user;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#remove-role">Remove role API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RemoveUserRole extends BaseActivity {
  private List<Long> userIds = List.of();
  private List<String> roles = List.of();
}
