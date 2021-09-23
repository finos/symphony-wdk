package com.symphony.bdk.workflow.swadl.v1.activity;

import com.symphony.bdk.workflow.swadl.v1.Event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

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
   * The event triggering the activity's execution,
   * if not set the activity is triggered automatically once the activity before it finishes.
   */
  @Nullable
  private Event on;

  @JsonProperty("if")
  @Nullable
  private String ifCondition;

  @JsonProperty("else")
  @Nullable
  private Object elseCondition;

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
