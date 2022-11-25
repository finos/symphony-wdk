package com.symphony.bdk.workflow.engine;

import lombok.experimental.UtilityClass;

import java.util.Locale;
import java.util.Objects;

/**
 * Helper class to extract the node type and group value from the given name value
 */
@UtilityClass
public class WorkflowNodeTypeHelper {
  private static final String EVENT = "EVENT";
  private static final String ACTIVITY = "ACTIVITY";
  private static final String GATEWAY = "GATEWAY";
  private static final String SUFFIX_EVENT = "_EVENT";
  private static final String SUFFIX_GATEWAY = "_GATEWAY";

  public String toType(String name) {
    if (name.endsWith(SUFFIX_EVENT)) {
      return name.substring(0, name.length() - 6);
    } else if (name.endsWith(SUFFIX_GATEWAY)) {
      return name.substring(0, name.length() - 8);
    }
    return name;
  }

  public String toGroup(String name) {
    if (name.endsWith(SUFFIX_EVENT)) {
      return EVENT;
    } else if (name.endsWith(SUFFIX_GATEWAY)) {
      return GATEWAY;
    }
    return ACTIVITY;
  }

  public String toUpperUnderscore(String name) {
    Objects.requireNonNull(name);
    StringBuilder sb = new StringBuilder();
    for (char c : name.toCharArray()) {
      if (Character.isUpperCase(c) && sb.length() != 0) {
        sb.append("_");
      }
      sb.append(c);
    }
    return sb.toString().toUpperCase(Locale.ROOT);
  }
}
