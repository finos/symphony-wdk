package com.symphony.bdk.workflow.util;

import com.symphony.bdk.gen.api.model.V4AttachmentInfo;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.lang.exception.NoAttachmentFoundException;

import java.util.List;

public class AttachmentsUtils {

  private AttachmentsUtils() {
  }

  public static List<V4AttachmentInfo> getAttachmentsFrom(RealTimeEvent<V4MessageSent> event) throws
      NoAttachmentFoundException {
    if (containsAttachments(event)) {
      return event.getSource().getMessage().getAttachments();
    }

    throw new NoAttachmentFoundException();
  }

  private static boolean containsAttachments(RealTimeEvent<V4MessageSent> event) {
    return event != null && event.getSource() != null
        && event.getSource().getMessage() != null
        && event.getSource().getMessage().getAttachments() != null
        && !event.getSource().getMessage().getAttachments().isEmpty();
  }

}
