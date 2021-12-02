package com.symphony.bdk.workflow.swadl.v1.activity.message;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.annotation.Nullable;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class UpdateMessage extends BaseActivity {
  private String messageId;
  @Nullable private String template;
  @Nullable private String content;


  public void setContent(Object content) {
    if (content instanceof Map) {
      setTemplate(((Map<String, String>) content).get("template"));
    } else if (content instanceof String) {
      this.content = (String) content;
    }
  }
}
