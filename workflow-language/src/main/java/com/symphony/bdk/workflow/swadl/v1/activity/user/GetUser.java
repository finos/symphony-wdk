package com.symphony.bdk.workflow.swadl.v1.activity.user;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#get-user-v2">Get user API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetUser extends BaseActivity {
  private String userId;
}
