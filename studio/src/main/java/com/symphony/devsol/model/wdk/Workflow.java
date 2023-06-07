package com.symphony.devsol.model.wdk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Workflow {
  private boolean active;
  private long createdBy;
}
