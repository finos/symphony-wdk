package com.symphony.bdk.workflow.swadl.v1.activity.connection;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class Connection extends BaseActivity {
  private String userId;
}
