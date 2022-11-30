package com.symphony.bdk.workflow.event;

import com.symphony.bdk.spring.events.RealTimeEvent;

import java.lang.reflect.ParameterizedType;

public interface RealTimeEventProcessor<T> {
  String EVENT_NAME_KEY = "eventName";

  @SuppressWarnings({"unchecked"})
  default Class<T> sourceType() {
    return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
  }

  void process(RealTimeEvent<T> event) throws Exception;
}
