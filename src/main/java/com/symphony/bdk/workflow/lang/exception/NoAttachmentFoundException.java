package com.symphony.bdk.workflow.lang.exception;

public class NoAttachmentFoundException extends RuntimeException {
  public NoAttachmentFoundException() {
    super("No attachment found in message.");
  }
}
