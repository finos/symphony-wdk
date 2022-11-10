package com.symphony.bdk.workflow.management;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowMgtAction;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

public interface WorkflowsMgtAction {

  void doAction(String content);

  WorkflowMgtAction action();
}
