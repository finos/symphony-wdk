package com.symphony.bdk.workflow.swadl.v1.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Random;

/**
 * A debugging activity.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Debug extends BaseActivity {
  @JsonProperty
  private Object object;

  public Debug() {
    this.setId(String.format("randomId-%s-%s", new Random().nextInt(100), System.currentTimeMillis() + ""));
  }
}

