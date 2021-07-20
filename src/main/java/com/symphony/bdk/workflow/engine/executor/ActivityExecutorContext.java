package com.symphony.bdk.workflow.engine.executor;

import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.stream.StreamService;

public interface ActivityExecutorContext<T> {
  void setOutputVariable(String activityId, String name, Object value);

  MessageService messages();

  StreamService streams();

  T getActivity();

}
