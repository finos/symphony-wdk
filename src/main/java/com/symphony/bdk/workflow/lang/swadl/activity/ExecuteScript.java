package com.symphony.bdk.workflow.lang.swadl.activity;

import com.symphony.bdk.workflow.engine.executor.CreateRoomExecutor;

import lombok.Data;

/**
 * A debugging/scripting activity using the Groovy script engine.
 */
@Data
public class ExecuteScript extends BaseActivity<CreateRoomExecutor> {
  // only supported script engine, close to Java, good enough for us
  public static final String SCRIPT_ENGINE = "groovy";

  private String script;
}

