package com.symphony.bdk.workflow.exceptions;

public class NoCommandToStartException extends RuntimeException {
  public NoCommandToStartException() {
    super("No command to start the workflow.");
  }
}
