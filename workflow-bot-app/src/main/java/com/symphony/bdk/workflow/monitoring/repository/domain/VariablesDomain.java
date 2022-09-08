package com.symphony.bdk.workflow.monitoring.repository.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@Data
@NoArgsConstructor
public class VariablesDomain {
  private Map<String, Object> outputs = Collections.emptyMap();
  private int revision;
  private Instant updateTime;
}
