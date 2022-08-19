package com.symphony.bdk.workflow.swadl.v1.activity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OboActivity extends BaseActivity {
  private Obo obo;
}
