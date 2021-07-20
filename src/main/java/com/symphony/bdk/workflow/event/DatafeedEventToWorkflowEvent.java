package com.symphony.bdk.workflow.event;

import com.symphony.bdk.workflow.engine.WorkflowEngine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DatafeedEventToWorkflowEvent {

  @Autowired
  protected WorkflowEngine workflowEngine;
}
