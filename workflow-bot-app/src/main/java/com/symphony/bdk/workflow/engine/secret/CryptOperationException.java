package com.symphony.bdk.workflow.engine.secret;

public class CryptOperationException extends RuntimeException {
  private static final long serialVersionUID = 698242968875054562L;

  public CryptOperationException(String s, Throwable e) {
    super(s, e);
  }
}
