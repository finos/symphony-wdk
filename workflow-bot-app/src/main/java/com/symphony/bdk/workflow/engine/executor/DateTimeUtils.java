package com.symphony.bdk.workflow.engine.executor;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import javax.annotation.Nullable;

public final class DateTimeUtils {

  private DateTimeUtils() {
    // utility class
  }

  /**
   * Null-returning one-liner to convert an ISO 8601 string to an epoch timestamp in ms.
   */
  @Nullable
  public static Long toEpochMilli(@Nullable String iso8601Ts) {
    if (iso8601Ts == null) {
      return null;
    }
    return Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(iso8601Ts)).toEpochMilli();
  }
}
