package com.symphony.bdk.workflow.logs;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogsStreamingAppender extends UnsynchronizedAppenderBase<ILoggingEvent> implements SmartLifecycle {

  private final LogsStreamingService service;

  @Override
  protected void append(ILoggingEvent eventObject) {
    StringBuilder formattedMessage = new StringBuilder();
    formattedMessage.append(eventObject.getFormattedMessage());

    if (eventObject.getThrowableProxy() != null) {
      formattedMessage.append("\n")
              .append(Arrays.stream(eventObject.getThrowableProxy().getStackTraceElementProxyArray())
                      .map(StackTraceElementProxy::getSTEAsString)
                      .collect(Collectors.joining("\n")));
    }


    service.broadcast(eventObject.getTimeStamp(), eventObject.getLevel().toString(), eventObject.getLoggerName(),
            formattedMessage.toString());
  }

  @Override
  public boolean isRunning() {
    return isStarted();
  }
}
