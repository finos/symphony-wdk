package com.symphony.bdk.workflow.swadl.v1.activity.group;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference/getgroup">Get group API</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GetGroup extends BaseActivity {
  private String groupId;
}
