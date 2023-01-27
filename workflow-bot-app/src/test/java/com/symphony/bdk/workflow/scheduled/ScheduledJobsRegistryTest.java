package com.symphony.bdk.workflow.scheduled;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
public class ScheduledJobsRegistryTest {

  @Mock
  private ScheduledThreadPoolExecutor poolExecutor;

  @InjectMocks
  private ScheduledJobsRegistry scheduledJobsRegistry;

  @Test
  void scheduleJobNotNullTest() {
    when(poolExecutor.schedule(any(RunnableScheduledJob.class), any(Long.class), any(TimeUnit.class))).thenReturn(null);
    when(poolExecutor.getQueue()).thenReturn(new LinkedBlockingQueue<>());

    scheduledJobsRegistry.scheduleJob(new RunnableScheduledJob(() -> "jobId", 1L, null));
    verify(poolExecutor).schedule(any(RunnableScheduledJob.class), eq(1L), eq(TimeUnit.SECONDS));
  }

  @Test
  void scheduleJobNullTest() {
    when(poolExecutor.getQueue()).thenReturn(new LinkedBlockingQueue<>());

    scheduledJobsRegistry.scheduleJob(null);
    verify(poolExecutor, never()).schedule(any(RunnableScheduledJob.class), any(Long.class), any(TimeUnit.class));
  }
}
