package com.symphony.bdk.workflow.logs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogsStreamingServiceTest {
  @Mock SseEmitter sseEmitter;
  LogsStreamingService service = new LogsStreamingService();

  @Test
  void broadcast_withSubscribedEmitter_successful() throws IOException {
    doNothing().when(sseEmitter).send(any(SseEmitter.SseEventBuilder.class));
    service.subscribe(sseEmitter);
    service.broadcast(Instant.now().toEpochMilli(), "DEBUG", "www", "log message");
    verify(sseEmitter).send(any(SseEmitter.SseEventBuilder.class));
  }

  @Test
  void broadcast_withSubscribedEmitter_failureThenUnsubscribe() throws IOException {
    doNothing().when(sseEmitter).completeWithError(any(AsyncRequestTimeoutException.class));
    doThrow(new AsyncRequestTimeoutException()).when(sseEmitter).send(any(SseEmitter.SseEventBuilder.class));
    service.subscribe(sseEmitter);
    service.broadcast(Instant.now().toEpochMilli(), "DEBUG", "www", "log message");
    verify(sseEmitter).send(any(SseEmitter.SseEventBuilder.class));
    verify(sseEmitter).completeWithError(any(AsyncRequestTimeoutException.class));
  }
}
