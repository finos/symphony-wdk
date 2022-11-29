package com.symphony.bdk.workflow.event;

import com.symphony.bdk.workflow.swadl.v1.Event;

import org.apache.commons.lang3.tuple.Triple;

public interface EventVisitor {

  boolean predict(Event event);

  Triple<String, String, Class<?>> getEventTripleInfo(Event event, String workflowId, String botName);
}
