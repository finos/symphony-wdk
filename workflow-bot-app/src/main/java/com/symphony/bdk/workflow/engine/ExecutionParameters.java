package com.symphony.bdk.workflow.engine;

import lombok.Value;

import java.util.Map;

@Value
public class ExecutionParameters {
  Map<String, Object> arguments;
  String token;
}
