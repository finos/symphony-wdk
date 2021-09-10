package com.symphony.bdk.workflow.engine.executor;

import java.io.IOException;

/**
 * Implement this interface to define your own activities.
 *
 * @param <T> The activity type used in the workflow's definition.
 */
public interface ActivityExecutor<T> {

  /**
   * Called when the activity is executed.
   *
   * @param context Gives access to the activity definition from the workflow,
   *                to contextual information such as variables
   *                as well as the BDK services.
   */
  void execute(ActivityExecutorContext<T> context) throws IOException;
}
