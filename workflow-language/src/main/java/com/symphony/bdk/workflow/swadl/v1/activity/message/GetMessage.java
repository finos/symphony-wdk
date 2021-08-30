package com.symphony.bdk.workflow.swadl.v1.activity.message;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @see <a href="https://developers.symphony.com/restapi/reference#get-message-v1">Get message API</a>
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class GetMessage extends BaseActivity {
  private String messageId;
}
