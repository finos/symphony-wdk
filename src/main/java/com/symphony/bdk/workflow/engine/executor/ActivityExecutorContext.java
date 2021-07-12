package com.symphony.bdk.workflow.engine.executor;

import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.stream.StreamService;

public interface ActivityExecutorContext<T> {
  String getVariable(String name);

  void setVariable(String name, Object value);

  //TODO: replace the method above with this one
  void setOutputVariable(String activityId, String name, Object value);

  MessageService messages();

  StreamService streams();

  T getActivity();

}
