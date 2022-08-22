package com.symphony.bdk.workflow.swadl.v1.activity.message;

import com.symphony.bdk.workflow.swadl.v1.activity.OboActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

@EqualsAndHashCode(callSuper = true)
@Data
public class SendMessage extends OboActivity {

  @Nullable private String template;
  @Nullable private String content;
  @Nullable private To to;
  @Nullable private List<Attachment> attachments;

  @SuppressWarnings("unchecked")
  public void setContent(Object content) {
    if (content instanceof Map) {
      setTemplate(((Map<String, String>) content).get("template"));
    } else if (content instanceof String) {
      this.content = (String) content;
    }
  }

  @Data
  public static class To {
    @Nullable private String streamId;
    @Nullable private List<String> streamIds;
    @Nullable private List<Long> userIds;
  }


  @Data
  public static class Attachment {
    @Nullable private String messageId;
    @Nullable private String attachmentId;
    @Nullable private String contentPath;
  }
}
