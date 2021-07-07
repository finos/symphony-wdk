package com.symphony.bdk.workflow.lang.swadl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Variable {
  private String key;
  private String value;
}
