package com.symphony.bdk.workflow.customassertion;

import com.symphony.bdk.workflow.swadl.v1.Workflow;

public class Assertions {
  public static WorkflowAssert assertThat(Workflow actual) {
    return new WorkflowAssert(actual);
  }
}
