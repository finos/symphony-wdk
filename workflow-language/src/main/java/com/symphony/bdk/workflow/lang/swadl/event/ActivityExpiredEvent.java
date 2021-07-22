package com.symphony.bdk.workflow.lang.swadl.event;

import lombok.Data;

@Data
public class ActivityExpiredEvent {
  private String id; // TODO change it to activity-id? (as it is a reference)
}
