package com.symphony.bdk.workflow.logs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class LogsStreamingService {
  private final List<SseEmitter> emitters = new ArrayList<>();

  @Async
  public void subscribe(SseEmitter emitter) {
    log.debug("subscribe a new sse emitter.");
    emitter.onCompletion(() -> this.emitters.remove(emitter));
    emitter.onTimeout(() -> {
      emitter.complete();
      this.emitters.remove(emitter);
    });
    this.emitters.add(emitter);
  }

  public void broadcast(long timestamp, String level, String logger, String data) {
    List<SseEmitter> failedConnections = new ArrayList<>();
    emitters.forEach(emitter -> {
      SseEmitter.SseEventBuilder event = SseEmitter.event()
          .id(DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(timestamp)))
          .data(String.format("[%s] %s - %s", level, logger, data))
          .name("message");
      boolean sent = sendMessage(event, emitter);
      if (!sent) {
        failedConnections.add(emitter);
      }
    });
    if (!failedConnections.isEmpty()) {
      unsubscribe(failedConnections);
    }
  }

  /**
   * @param event   log event
   * @param emitter emitter used to send message
   * @return message sent successfully
   */
  private boolean sendMessage(SseEmitter.SseEventBuilder event, SseEmitter emitter) {
    try {
      emitter.send(event);
      return true;
    } catch (Exception ex) {
      log.trace("send message with failure - {}", ex.getMessage());
      emitter.completeWithError(ex);
      return false;
    }
  }

  private void unsubscribe(List<SseEmitter> ids) {
    emitters.removeAll(ids);
  }
}
