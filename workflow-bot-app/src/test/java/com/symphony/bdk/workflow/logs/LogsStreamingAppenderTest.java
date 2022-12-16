package com.symphony.bdk.workflow.logs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

@ExtendWith(MockitoExtension.class)
class LogsStreamingAppenderTest {
  @Mock
  LogsStreamingService service;
  @InjectMocks
  LogsStreamingAppender appender;

  @Test
  void append_eventBroadcast() {
    ILoggingEvent event = mock(ILoggingEvent.class);
    when(event.getTimeStamp()).thenReturn(Instant.now().toEpochMilli());
    when(event.getLoggerName()).thenReturn("www");
    when(event.getLevel()).thenReturn(Level.DEBUG);
    when(event.getFormattedMessage()).thenReturn("log message");
    doNothing().when(service).broadcast(anyLong(), anyString(), anyString(), anyString());
    appender.append(event);
    verify(service).broadcast(anyLong(), anyString(), anyString(), anyString());
  }

  @Test
  void isRunning() {
    assertThat(appender.isRunning()).isFalse();
  }
}
