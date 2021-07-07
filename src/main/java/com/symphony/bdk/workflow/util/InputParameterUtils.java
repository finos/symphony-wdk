package com.symphony.bdk.workflow.util;

import java.util.List;

public class InputParameterUtils {

  private InputParameterUtils() {
  }

  public static String longListToString(List<Long> longs) {
    if (longs == null) {
      return null;
    }

    return String.valueOf(longs)
        .replace("[", "")
        .replace("]", "")
        .replaceAll("\\s+", "");
  }
}
