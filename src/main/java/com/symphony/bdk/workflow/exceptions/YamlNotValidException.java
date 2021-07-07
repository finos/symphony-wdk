package com.symphony.bdk.workflow.exceptions;

import java.io.IOException;

/**
 * An exception to be thrown when validating an invalid YAML file.
 */
public class YamlNotValidException extends IOException {
  public YamlNotValidException() {
    super("YAML file is not valid.");
  }
}
