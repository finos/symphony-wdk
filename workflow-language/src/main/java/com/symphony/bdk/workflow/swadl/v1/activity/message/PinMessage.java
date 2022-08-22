package com.symphony.bdk.workflow.swadl.v1.activity.message;

import com.symphony.bdk.workflow.swadl.v1.activity.OboActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PinMessage extends OboActivity {
  private String messageId;
}
