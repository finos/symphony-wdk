package com.symphony.bdk.workflow.swadl.v1.activity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class SendMessage extends BaseActivity {
  private To to;
  private String content;
  private List<Attachment> attachments;

  @Data
  public static class To {
    private String streamId;
  }

  @Data
  public static class Attachment {
    private String messageId;
    private String attachmentId;
    private String contentPath;
  }

}
