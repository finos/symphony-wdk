package com.symphony.bdk.workflow.swadl.v1.activity;

import com.symphony.bdk.workflow.swadl.v1.Event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * Base implementation of an activity providing data shared across all activities.
 */
@Data
public abstract class BaseActivity {
  /**
   * A unique identifier for an activity. Using camelCase.
   */
  private String id;

  /**
   * A short name to describe the activity.
   */
  private String name;

  /**
   * A longer text to describe the activity.
   */
  private String description;

  /**
   * The event triggering the activity's execution,
   * if not set the activity is triggered automatically once the activity before it finishes.
   */
  private Event on;

  @JsonProperty("if")
  private String ifCondition;

  @JsonProperty("else")
  private Object elseCondition;

  /**
   * We usually want unset fields to stay null as the API is usually using default values.
   *
   * @param field a Boolean value.
   * @return null if field is null.
   */
  protected static Boolean toBoolean(String field) {
    return field == null ? null : Boolean.valueOf(field);
  }

  /**
   * We usually want unset fields to stay null as the API is usually using default values.
   *
   * @param field an Integer value.
   * @return null if field is null.
   */
  protected static Integer toInt(String field) {
    return field == null ? null : Integer.valueOf(field);
  }


  @JsonIgnore
  public List<Event> getEvents() {
    if (on != null && on.getOneOf() != null) {
      return on.getOneOf();
    } else if (on != null) {
      return Collections.singletonList(on);
    } else {
      return Collections.emptyList();
    }
  }
}
