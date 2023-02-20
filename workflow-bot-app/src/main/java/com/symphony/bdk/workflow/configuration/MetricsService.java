package com.symphony.bdk.workflow.configuration;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

@Component
@Slf4j
@AllArgsConstructor
public class MetricsService {
  private static final String JVM_THREADS_STATES = "jvm.threads.states";
  private static final String JVM_MEMORY_USED = "jvm.memory.used";
  private static final String PROCESS_CPU_USAGE = "process.cpu.usage";
  private static final String PROCESS_FILES_OPEN = "process.files.open";
  private static final String PROCESS_FILES_MAX = "process.files.max";
  private static final String TERMINATED = "terminated";
  private static final String BLOCKED = "blocked";
  private static final String RUNNABLE = "runnable";
  private static final String WAITING = "waiting";
  private static final String STATE = "state";

  private final MeterRegistry registry;

  //@Scheduled(fixedRate = 20, timeUnit = TimeUnit.SECONDS)
  public void logMetrics() {
    double jvmMemoryUsed = registry.get(JVM_MEMORY_USED).gauge().value();
    double jvmCpuUsage = registry.get(PROCESS_CPU_USAGE).gauge().value();
    double jvmOpenFiles = registry.get(PROCESS_FILES_OPEN).gauge().value();
    double jvmMaxFiles = registry.get(PROCESS_FILES_MAX).gauge().value();
    String jvmMemoryBaseUnit = registry.get(JVM_MEMORY_USED).gauge().getId().getBaseUnit();

    double terminatedThreads =
        registry.get(JVM_THREADS_STATES).tag(STATE, TERMINATED).gauge().value();
    double blockedThreads = registry.get(JVM_THREADS_STATES).tag(STATE, BLOCKED).gauge().value();
    double runnableThreads =
        registry.get(JVM_THREADS_STATES).tag(STATE, RUNNABLE).gauge().value();
    double waitingThreads = registry.get(JVM_THREADS_STATES).tag(STATE, WAITING).gauge().value();

    log.info("JVM MEMORY USED {}", jvmMemoryUsed);
    log.info("JVM CPU USAGE {}", jvmCpuUsage);
    log.info("JVM OPEN FILES {}", jvmOpenFiles);
    log.info("JVM MAX FILES {}", jvmMaxFiles);
    log.info("JVM MEMORY BASE UNIT {}", jvmMemoryBaseUnit);
    log.info("TERMINATED THREADS {}", terminatedThreads);
    log.info("BLOCKED THREADS {}", blockedThreads);
    log.info("RUNNABLE THREADS {}", runnableThreads);
    log.info("WAITING THREADS {}", waitingThreads);


    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    System.out.println(String.format("Initial memory: %.2f GB",
        (double)memoryMXBean.getHeapMemoryUsage().getInit() /1073741824));
    System.out.println(String.format("Used heap memory: %.2f GB",
        (double)memoryMXBean.getHeapMemoryUsage().getUsed() /1073741824));
    System.out.println(String.format("Max heap memory: %.2f GB",
        (double)memoryMXBean.getHeapMemoryUsage().getMax() /1073741824));
    System.out.println(String.format("Committed memory: %.2f GB",
        (double)memoryMXBean.getHeapMemoryUsage().getCommitted() /1073741824));

    ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    for(Long threadID : threadMXBean.getAllThreadIds()) {
      ThreadInfo info = threadMXBean.getThreadInfo(threadID);
      System.out.println("Thread name: " + info.getThreadName());
      System.out.println("Thread State: " + info.getThreadState());
      System.out.println(String.format("CPU time: %s ns",
          threadMXBean.getThreadCpuTime(threadID)));
    }

  }
}
