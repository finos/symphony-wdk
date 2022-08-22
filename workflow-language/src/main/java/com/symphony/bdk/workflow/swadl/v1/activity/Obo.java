package com.symphony.bdk.workflow.swadl.v1.activity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.annotation.Nullable;

@EqualsAndHashCode
@Data
public class Obo {
  @Nullable private String username;
  @Nullable private Long userId;
}
