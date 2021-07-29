package com.symphony.bdk.workflow.engine.executor;

import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.workflow.swadl.v1.activity.BaseActivity;

public interface ActivityExecutorContext<T extends BaseActivity> {

  /**
   * ${activityId.outputs.myOutput}
   */
  String OUTPUTS = "outputs";

  /**
   * ${variables.myVariable}
   */
  String VARIABLES = "variables";

  /**
   * ${event.source....}
   * ${event.initiator....}
   */
  String EVENT = "event";

  /**
   * Define an output variable that can be retrieved later with ${activityId.outputs.name}.
   */
  void setOutputVariable(String name, Object value);

  /**
   * @return BDK service to send and manipulate messages.
   */
  MessageService messages();

  /**
   * @return BDK service to manage streams (aka rooms).
   */
  StreamService streams();

  /**
   * @return The activity definition from the workflow.
   */
  T getActivity();

  EventHolder<Object> getEvent();
}
