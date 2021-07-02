package com.symphony.bdk.workflow.exceptions;

import java.io.IOException;

/**
 * An exception to be thrown when validating an invalid YAML file.
 */
public class YAMLNotValidException extends IOException  {
  public YAMLNotValidException() {
    super("YAML file is not valid.");
  }
}
