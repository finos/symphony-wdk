package com.symphony.bdk.workflow.lang;

import com.symphony.bdk.workflow.lang.swadl.activity.BaseActivity;

import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class ActivityRegistry {

  private static Set<Class<? extends BaseActivity>> activityTypes;

  static {
    Reflections reflections = new Reflections(new ConfigurationBuilder()
        .setScanners(new SubTypesScanner(false))
        // TODO how to make it work for everybody and fast
        .addUrls(ClasspathHelper.forPackage("com.symphony"))
        .addUrls(ClasspathHelper.forPackage("org.acme")));
    activityTypes = reflections.getSubTypesOf(BaseActivity.class);

    log.info("Found these activities: {}", activityTypes);
  }

  public static Set<Class<? extends BaseActivity>> getActivityTypes() {
    return new HashSet<>(activityTypes);
  }
}
