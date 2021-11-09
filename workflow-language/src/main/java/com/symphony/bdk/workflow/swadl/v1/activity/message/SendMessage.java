package com.symphony.bdk.workflow.swadl.v1.activity.message;

import com.symphony.bdk.workflow.swadl.v1.Variable;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import javax.annotation.Nullable;

@EqualsAndHashCode(callSuper = true)
@Data
public class SendMessage extends BaseActivity {
  private Object content;
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


  @Data
  public static class Content {
    @Nullable private String template;
  }
}
