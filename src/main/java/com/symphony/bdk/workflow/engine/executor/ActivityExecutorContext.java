package com.symphony.bdk.workflow.engine.executor;

import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.stream.StreamService;

public interface ActivityExecutorContext<T> {
  String getVariable(String name);

  void setVariable(String name, String value);

  MessageService messages();

  StreamService streams();

  T getActivity();

}
