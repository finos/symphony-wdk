package com.symphony.bdk.workflow.lang.exception;

public class NoCommandToStartException extends RuntimeException {
  public NoCommandToStartException() {
    super("No command to start the workflow.");
  }
}
