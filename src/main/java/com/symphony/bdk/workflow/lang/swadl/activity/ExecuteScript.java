package com.symphony.bdk.workflow.lang.swadl.activity;

import com.symphony.bdk.workflow.engine.executor.CreateRoomExecutor;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * A debugging/scripting activity using the Groovy script engine.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ExecuteScript extends BaseActivity<CreateRoomExecutor> {
  // only supported script engine, close to Java, good enough for us
  public static final String SCRIPT_ENGINE = "groovy";

  private String script;
}

