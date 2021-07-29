package com.symphony.bdk.workflow.swadl.v1.activity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * A debugging/scripting activity using the Groovy script engine.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ExecuteScript extends BaseActivity { // no executor it is handled as a script task in Camunda
  // only supported script engine, close to Java, good enough for us
  public static final String SCRIPT_ENGINE = "groovy";

  private String script;
}

