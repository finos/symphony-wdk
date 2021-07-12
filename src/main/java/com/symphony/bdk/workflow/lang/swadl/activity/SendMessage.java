package com.symphony.bdk.workflow.lang.swadl.activity;

import com.symphony.bdk.workflow.engine.executor.SendMessageExecutor;

import lombok.Data;

@Data
public class SendMessage extends BaseActivity<SendMessageExecutor> {
  private To to;
  private String content;

  @Data
  public static class To {
    private String streamId;
  }
}
