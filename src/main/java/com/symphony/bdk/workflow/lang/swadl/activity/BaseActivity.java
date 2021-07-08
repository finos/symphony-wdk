package com.symphony.bdk.workflow.lang.swadl.activity;

import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.lang.swadl.Event;

import lombok.Data;

@Data
public abstract class BaseActivity<T extends ActivityExecutor<?>> {
  private String id;
  private String name;
  private String description;
  private Event on;
}
