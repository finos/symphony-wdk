package com.symphony.bdk.workflow.lang.swadl.activity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SendMessage extends BaseActivity {
  private To to;
  private String content;

  @Data
  public static class To {
    private String streamId;
  }
}
