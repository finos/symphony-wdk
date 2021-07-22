package com.symphony.bdk.workflow;

import com.symphony.bdk.workflow.lang.swadl.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DoSomething extends BaseActivity {
  private String myParameter;
}
