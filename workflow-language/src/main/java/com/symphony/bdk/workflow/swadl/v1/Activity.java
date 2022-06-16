package com.symphony.bdk.workflow.swadl.v1;

import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;
import com.symphony.bdk.workflow.swadl.v1.activity.RelationalEvents;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Optional;

/**
 * To define activities while supporting typing with JSON schema we handle them as nested objects:
 *
 * <pre>
 * activities:
 *  - activity-type:
 *      activity-field1: data
 *      activity-field2: data
 * </pre>
 *
 * <p>This is handled by a specific deserializer when processing the workflow. This also allows to support
 * activity types found at runtime (i.e. custom activities).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Activity {

  private BaseActivity implementation;

  public BaseActivity getActivity() {
    return implementation;
  }

  public Optional<Event> getEvent() {
    return Optional.ofNullable(getActivity().getOn());
  }

  public RelationalEvents getEvents() {
    return getActivity().getEvents();
  }
}

