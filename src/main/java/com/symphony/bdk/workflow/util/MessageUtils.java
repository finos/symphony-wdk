package com.symphony.bdk.workflow.util;

import com.symphony.bdk.core.service.message.exception.PresentationMLParserException;
import com.symphony.bdk.core.service.message.util.PresentationMLParser;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.spring.events.RealTimeEvent;

public class MessageUtils {

  public static String getStreamIdFrom(RealTimeEvent<V4MessageSent> event) throws Exception {
    if (event != null && event.getSource() != null
        && event.getSource().getMessage() != null
        && event.getSource().getMessage().getStream() != null
        && event.getSource().getMessage().getStream().getStreamId() != null) {
      return event.getSource().getMessage().getStream().getStreamId();
    }

    throw new Exception();
  }

  public static String getMessageIdFrom(RealTimeEvent<V4MessageSent> event) throws Exception {
    if (event != null && event.getSource() != null
        && event.getSource().getMessage() != null
        && event.getSource().getMessage().getMessageId() != null) {
      return event.getSource().getMessage().getMessageId();
    }

    throw new Exception();
  }

  public static String getMessageTextContent(RealTimeEvent<V4MessageSent> event) throws Exception {
    try {
      V4Message message = event.getSource().getMessage();

      return PresentationMLParser.getTextContent(message.getMessage());
    } catch (PresentationMLParserException e) {
      throw new Exception();
    }
  }
}
