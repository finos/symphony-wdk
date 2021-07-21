package com.symphony.bdk.workflow.event;

import com.symphony.bdk.workflow.engine.WorkflowEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DatafeedEventToWorkflowEvent {

  protected static final Logger LOGGER = LoggerFactory.getLogger(DatafeedEventToWorkflowEvent.class);

  @Autowired
  protected WorkflowEngine workflowEngine;
}
