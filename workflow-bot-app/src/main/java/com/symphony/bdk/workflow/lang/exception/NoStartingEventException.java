package com.symphony.bdk.workflow.lang.exception;

public class NoStartingEventException extends RuntimeException {
  public NoStartingEventException() {
    super("Workflow does not have any starting event.");
  }
}
