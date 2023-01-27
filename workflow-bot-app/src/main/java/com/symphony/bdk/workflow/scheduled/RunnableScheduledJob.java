package com.symphony.bdk.workflow.scheduled;

import com.symphony.bdk.http.api.tracing.MDCUtils;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
@Generated // not testes
public class RunnableScheduledJob implements Runnable, ScheduledJob {

  @Getter
  private final Id id;

  @Getter
  private final long delay;

  private final Runnable job;

  @Override
  public void run() {
    try {
      MDCUtils.wrap(this.job).run();
    } catch (Throwable throwable) {
      log.error("RunnableScheduledJob ran into exception - [{}]", throwable.getMessage());
      log.trace("", throwable);
    }
  }

  @FunctionalInterface
  public interface Id {
    String id();
  }
}
