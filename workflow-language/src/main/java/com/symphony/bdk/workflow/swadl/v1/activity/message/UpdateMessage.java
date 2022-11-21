package com.symphony.bdk.workflow.swadl.v1.activity.message;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;
import javax.annotation.Nullable;

@EqualsAndHashCode(callSuper = true)
@Data
public class UpdateMessage extends BaseActivity {
  private String messageId;
  @Nullable private String template;
  @Nullable private String content;
  private Boolean silent = Boolean.TRUE;

  @SuppressWarnings("unchecked")
  public void setContent(Object content) {
    if (content instanceof Map) {
      Map<String, String> map = (Map<String, String>) content;
      setTemplate(map.get("template"));
    } else if (content instanceof String) {
      this.content = (String) content;
    }
  }
}
