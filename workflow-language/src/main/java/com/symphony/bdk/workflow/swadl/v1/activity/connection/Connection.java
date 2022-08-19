package com.symphony.bdk.workflow.swadl.v1.activity.connection;

import com.symphony.bdk.workflow.swadl.v1.activity.Obo;
import com.symphony.bdk.workflow.swadl.v1.activity.OboActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class Connection extends OboActivity {
  private String userId;
  private Obo obo;
}
