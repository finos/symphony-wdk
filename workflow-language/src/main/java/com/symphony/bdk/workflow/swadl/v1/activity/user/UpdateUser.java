package com.symphony.bdk.workflow.swadl.v1.activity.user;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#update-user-v2">Update user API</a>
 * @see <a href="https://developers.symphony.com/restapi/reference#update-user-status">Update user API</a>
 * @see <a href="https://developers.symphony.com/restapi/reference#update-features">Update user features API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UpdateUser extends CreateUser {
  private String userId;
}
