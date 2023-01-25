package com.symphony.bdk.workflow.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ScheduledJobsRegistry {
  private final ScheduledThreadPoolExecutor poolExecutor;

  public ScheduledJobsRegistry() {
    //TODO: make poolSize configurable
    this.poolExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(20);
  }

  public void scheduleJob(RunnableScheduledJob job) {
    if (job != null) {
      log.debug("Schedule job with id [{}]", job.getId().id());
      this.poolExecutor.schedule(job, job.getDelay(), TimeUnit.SECONDS);
    }

    log.debug("Current number of scheduled jobs in the executor pool is [{}]", poolExecutor.getQueue().size());
  }

  public void destroy() {
    this.poolExecutor.shutdown();
  }

}
