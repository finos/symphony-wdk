package com.symphony.bdk.workflow.swadl.v1.activity.attachment;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class GetAttachment extends BaseActivity {
  private String messageId;
  private String attachmentId;
}
