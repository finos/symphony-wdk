package com.symphony.bdk.workflow.engine.executor.request.client;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Response {
  private int code;
  private Object content;
}
