package com.symphony.bdk.workflow.swadl.exception;

import java.util.List;

public class UniqueIdViolationException extends RuntimeException {
  public UniqueIdViolationException(String workflowId, List<String> duplicatedIds) {
    super(String.format("These ids %s are duplicated in more than one activity in workflow %s", duplicatedIds,
        workflowId));
  }
}
