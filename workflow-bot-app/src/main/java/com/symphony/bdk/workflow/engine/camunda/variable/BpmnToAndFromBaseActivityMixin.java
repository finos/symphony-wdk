package com.symphony.bdk.workflow.engine.camunda.variable;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Specify how we want to serialize/deserialize a {@link com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity}
 * to and from BPMN.
 * <p/>
 * We don't want the variableProperties to appear but instead to flatten out what inside (so it can be mapped back to
 * the real activity properties).
 */
public abstract class BpmnToAndFromBaseActivityMixin {

  @JsonAnyGetter
  @JsonIgnore
  @JsonProperty
  Map<String, Object> variableProperties;

}
