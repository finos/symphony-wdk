package com.symphony.bdk.workflow.event;

import com.symphony.bdk.workflow.swadl.v1.Event;

import org.apache.commons.lang3.tuple.Triple;

public interface EventVisitor {

  boolean predict(Event event);

  /**
   * Triple, left value is the optional custom defined event id, which might be null;
   * middle is the event name; right is the event type
   *
   * @param event      current event, from which the triple info is extract
   * @param workflowId id of workflow
   * @param botName    bot display name
   * @return info in triple
   */
  Triple<String, String, Class<?>> getEventTripleInfo(Event event, String workflowId, String botName);
}
