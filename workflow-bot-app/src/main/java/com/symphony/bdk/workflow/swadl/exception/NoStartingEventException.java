package com.symphony.bdk.workflow.swadl.exception;

public class NoStartingEventException extends RuntimeException {
  public NoStartingEventException() {
    super("Workflow does not have any starting event.");
  }
}
