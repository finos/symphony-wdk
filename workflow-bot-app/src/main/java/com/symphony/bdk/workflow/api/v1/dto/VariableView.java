package com.symphony.bdk.workflow.api.v1.dto;

import com.symphony.bdk.workflow.monitoring.repository.domain.VariablesDomain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VariableView {
  private Map<String, Object> outputs;
  private int revision;
  private Instant updateTime;

  public VariableView(VariablesDomain domain) {
    if (domain != null) {
      this.setOutputs(domain.getOutputs());
      this.revision = domain.getRevision();
      this.updateTime = domain.getUpdateTime();
    }
  }
}
