package com.symphony.bdk.workflow.logs;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogsStreamingAppenderTest {
  @Mock
  LogsStreamingService service;
  @InjectMocks
  LogsStreamingAppender appender;

  @Test
  void append_eventBroadcast_withStackTrace() {
    ILoggingEvent event = mock(ILoggingEvent.class);
    when(event.getTimeStamp()).thenReturn(Instant.now().toEpochMilli());
    when(event.getLoggerName()).thenReturn("www");
    when(event.getLevel()).thenReturn(Level.DEBUG);
    when(event.getFormattedMessage()).thenReturn("log message\n");

    StackTraceElement stackTraceElement = new StackTraceElement("class", "method", "filename", 1);
    Throwable throwable = new Throwable();
    throwable.setStackTrace(new StackTraceElement[] {stackTraceElement});
    ThrowableProxy throwableProxy = new ThrowableProxy(throwable);
    when(event.getThrowableProxy()).thenReturn(throwableProxy);

    doNothing().when(service).broadcast(anyLong(), anyString(), anyString(), anyString());
    appender.append(event);
    verify(service).broadcast(anyLong(), anyString(), eq("www"),
        eq("log message\tjava.lang.Throwable: null\t\tat class.method(filename:1)\t"));
  }

  @Test
  void append_eventBroadcast_noStackTrace() {
    ILoggingEvent event = mock(ILoggingEvent.class);
    when(event.getTimeStamp()).thenReturn(Instant.now().toEpochMilli());
    when(event.getLoggerName()).thenReturn("www");
    when(event.getLevel()).thenReturn(Level.DEBUG);
    when(event.getFormattedMessage()).thenReturn("log message");
    doNothing().when(service).broadcast(anyLong(), anyString(), anyString(), anyString());
    appender.append(event);
    verify(service).broadcast(anyLong(), anyString(), eq("www"), eq("log message"));
  }

  @Test
  void isRunning() {
    assertThat(appender.isRunning()).isFalse();
  }
}
