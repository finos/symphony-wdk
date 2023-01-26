package com.symphony.bdk.workflow.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ScheduledJobsRegistry {
  private final ScheduledThreadPoolExecutor poolExecutor;

  public ScheduledJobsRegistry(@Value("${wdk.properties.schedule.pool-size}") int corePoolSize) {
    this.poolExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(corePoolSize);
  }

  public void scheduleJob(RunnableScheduledJob job) {
    if (job != null) {
      log.debug("Schedule job with id [{}]", job.getId().id());
      this.poolExecutor.schedule(job, job.getDelay(), TimeUnit.SECONDS);
    }

    log.debug("Current number of scheduled jobs in the executor pool is [{}]", poolExecutor.getQueue().size());
  }
}
