package com.symphony.bdk.workflow.context;

import com.symphony.bdk.workflow.lang.swadl.Workflow;

import lombok.Value;

@Value
public class WorkflowContext {
  private String streamId;
  private String messageId;
  private String attachmentId;
  private String contentMessage;
  private Workflow workflow;
}
