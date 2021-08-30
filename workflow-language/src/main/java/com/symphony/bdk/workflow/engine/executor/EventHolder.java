package com.symphony.bdk.workflow.engine.executor;

import com.symphony.bdk.gen.api.model.V4Initiator;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Similar to RealTimeEvent from the BDK but without the dependency on Spring so it can be serialized.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventHolder<T> {
  private V4Initiator initiator;

  // to preserve the generic typing while serializing/deserializing
  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@eventImpl")
  private T source;

  /**
   * Extra arguments associated with the event. Used for message received events.
   */
  private Map<String, Object> args;

}
