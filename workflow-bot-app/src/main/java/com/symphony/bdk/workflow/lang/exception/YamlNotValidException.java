package com.symphony.bdk.workflow.lang.exception;

import java.io.IOException;

/**
 * An exception to be thrown when validating an invalid YAML file.
 */
public class YamlNotValidException extends IOException {
  public YamlNotValidException(String report) {
    super("YAML file is not valid: " + report);
  }
}
