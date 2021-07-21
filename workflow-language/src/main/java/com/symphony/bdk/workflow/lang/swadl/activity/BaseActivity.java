package com.symphony.bdk.workflow.lang.swadl.activity;

import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.lang.swadl.Event;

import lombok.Data;

/**
 * Base implementation of an activity providing data shared across all activities.
 *
 * @param <T> The corresponding executor called when this activity is executed.
 */
@Data
public abstract class BaseActivity<T extends ActivityExecutor<?>> {
  /**
   * A unique identifier for an activity. Using camelCase.
   */
  private String id;

  /**
   * A short name to describe the activity.
   */
  private String name;

  /**
   * A longer text to describe the activity.
   */
  private String description;

  /**
   * The event triggering the activity's execution,
   * if not set the activity is triggered automatically once the activity before it finishes.
   */
  private Event on;
}
