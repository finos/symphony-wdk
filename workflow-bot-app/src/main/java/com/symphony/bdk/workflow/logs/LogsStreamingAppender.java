package com.symphony.bdk.workflow.logs;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogsStreamingAppender extends UnsynchronizedAppenderBase<ILoggingEvent> implements SmartLifecycle {

  private final LogsStreamingService service;

  @Override
  protected void append(ILoggingEvent eventObject) {
    StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append(eventObject.getFormattedMessage());

    if (eventObject.getThrowableProxy() != null) {
      messageBuilder.append("\n");
      messageBuilder.append(ThrowableProxyUtil.asString(eventObject.getThrowableProxy()));
    }

    String formattedMessage = messageBuilder.toString().replaceAll("(\\r|\\n|\\r\\n)+", "\t");
    service.broadcast(eventObject.getTimeStamp(), eventObject.getLevel().toString(), eventObject.getLoggerName(),
        formattedMessage);
  }

  @Override
  public boolean isRunning() {
    return isStarted();
  }
}
