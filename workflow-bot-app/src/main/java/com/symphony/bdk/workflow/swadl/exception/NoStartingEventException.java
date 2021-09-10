package com.symphony.bdk.workflow.swadl.exception;

public class NoStartingEventException extends RuntimeException {
  public NoStartingEventException(String workflowId) {
    super(String.format("Workflow with id \"%s\" does not have any starting event.", workflowId));
  }
}
