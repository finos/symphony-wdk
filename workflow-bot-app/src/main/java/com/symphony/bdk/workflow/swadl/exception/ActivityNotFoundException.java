package com.symphony.bdk.workflow.swadl.exception;

/**
 * An exception to be thrown when a non-existing activity id is referenced.
 */
public class ActivityNotFoundException extends RuntimeException {
  public ActivityNotFoundException(String workflowId, String unfoundActivityId, String referencingActivityId) {
    super(
        String.format("Invalid activity in the workflow %s: No activity found with id %s referenced in %s", workflowId,
            unfoundActivityId, referencingActivityId));
  }
}
