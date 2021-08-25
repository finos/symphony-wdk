package com.symphony.bdk.workflow.customAssertion;

import com.symphony.bdk.workflow.swadl.v1.Workflow;

public class Assertions {
  public static WorkflowAssert assertThat(Workflow actual) {
    return new WorkflowAssert(actual);
  }
}
