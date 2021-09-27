package com.symphony.bdk.workflow.swadl.v1.activity;

import com.symphony.bdk.workflow.swadl.v1.Variable;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import javax.annotation.Nullable;

@EqualsAndHashCode(callSuper = true)
@Data
public class SendMessage extends BaseActivity {
  private String content;
  @Nullable private To to;
  @Nullable private List<Attachment> attachments;


  @Data
  public static class To {
    @Nullable private String streamId;
    @Nullable private Variable<List<String>> streamIds;
    @Nullable private Variable<List<Number>> userIds;
  }


  @Data
  public static class Attachment {
    @Nullable private String messageId;
    @Nullable private String attachmentId;
    @Nullable private String contentPath;
  }

}
