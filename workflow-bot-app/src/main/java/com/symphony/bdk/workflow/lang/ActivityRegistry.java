package com.symphony.bdk.workflow.lang;

import com.symphony.bdk.workflow.lang.swadl.activity.BaseActivity;

import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ActivityRegistry {

  private static Set<Class<? extends BaseActivity>> activityTypes;

  static {
    log.info(ClasspathHelper.forClassLoader().toString());
    Reflections reflections = new Reflections(new ConfigurationBuilder()
        .setScanners(new SubTypesScanner(false))
        // this is a bit ugly but it works faster than scanning the entire classpath and for all contexts (JAR, tests)
        .addUrls(ClasspathHelper.forClassLoader().stream()
            // avoid bot's dependencies / pick only lib/ folder
            .filter(a -> a.toString().contains("lib/") && !a.toString().contains("BOOT-INF"))
            .collect(Collectors.toList()))
        .addUrls(ClasspathHelper.forPackage("com.symphony.bdk.workflow")));
    activityTypes = reflections.getSubTypesOf(BaseActivity.class);

    log.info("Found these activities: {}", activityTypes);
  }

  public static Set<Class<? extends BaseActivity>> getActivityTypes() {
    return new HashSet<>(activityTypes);
  }
}
