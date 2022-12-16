package com.symphony.bdk.workflow.logs;

import ch.qos.logback.classic.spi.ILoggingEvent;
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
    service.broadcast(eventObject.getTimeStamp(), eventObject.getLevel().toString(), eventObject.getLoggerName(),
        eventObject.getFormattedMessage());
  }

  @Override
  public boolean isRunning() {
    return isStarted();
  }
}
