package com.symphony.bdk.workflow.swadl;

import com.symphony.bdk.workflow.engine.executor.ActivityExecutor;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public final class ActivityRegistry {

  private static final Set<Class<? extends BaseActivity>> activityTypes;
  private static final Map<Class<? extends BaseActivity>, Class<? extends ActivityExecutor<? extends BaseActivity>>>
      activityExecutors;

  static {
    Reflections reflections = new Reflections(new ConfigurationBuilder()
        .setScanners(new SubTypesScanner(false))
        // this is a bit ugly but it works faster than scanning the entire classpath and for all contexts (JAR, tests)
        .addUrls(ClasspathHelper.forClassLoader().stream()
            // avoid bot's dependencies / pick only lib/ folder
            .filter(a -> a.toString().contains("lib/") && !a.toString().contains("BOOT-INF"))
            .collect(Collectors.toList()))
        .addUrls(ClasspathHelper.forPackage("com.symphony.bdk.workflow")));
    activityTypes = reflections.getSubTypesOf(BaseActivity.class);

    activityExecutors = reflections.getSubTypesOf(ActivityExecutor.class).stream()
        .map(a -> (Class<? extends ActivityExecutor<? extends BaseActivity>>) a)
        .collect(Collectors.toMap(ActivityRegistry::findMatchingActivity, Function.identity()));

    log.info("Found these activities: {} and executors: {}", activityTypes, activityExecutors);
  }

  private static Class<? extends BaseActivity> findMatchingActivity(
      Class<? extends ActivityExecutor<? extends BaseActivity>> a) {
    try {
      Type activityType = TypeUtils.getTypeArguments(a, ActivityExecutor.class).values().stream()
          .filter(arg -> {
            try {
              return BaseActivity.class.isAssignableFrom(Class.forName(arg.getTypeName()));
            } catch (ClassNotFoundException e) {
              throw new IllegalStateException("Executor " + a + " should implement ActivityExecutor<Activity>");
            }
          })
          .findFirst()
          .orElseThrow();
      return (Class<? extends BaseActivity>) Class.forName(activityType.getTypeName());
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  private ActivityRegistry() {
  }

  public static Set<Class<? extends BaseActivity>> getActivityTypes() {
    return new HashSet<>(activityTypes);
  }

  public static Map<Class<? extends BaseActivity>,
      Class<? extends ActivityExecutor<? extends BaseActivity>>> getActivityExecutors() {
    return activityExecutors;
  }
}
