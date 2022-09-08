package com.symphony.bdk.workflow.monitoring.repository.domain;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
public class WorkflowDomain {
  private final String id;
  private final String name;
  private final int version;
}
