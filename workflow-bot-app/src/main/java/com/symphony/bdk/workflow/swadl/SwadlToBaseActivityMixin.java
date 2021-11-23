package com.symphony.bdk.workflow.swadl;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Specify how we want to deserialize an activity from SWADL.
 * We put all the activity properties inside variableProperties,
 * so we can at this stage use ${variables} instead of the real types.
 */
public interface SwadlToBaseActivityMixin {

  @JsonAnySetter
  @JsonProperty
  Map<String, Object> variableProperties();

  @JsonAnySetter
  void add(String key, Object value);
}
