package com.symphony.bdk.workflow.engine.executor;

import com.symphony.bdk.core.service.connection.ConnectionService;

import java.io.IOException;
import java.io.InputStream;

public interface ActivityExecutorContext<T> {

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
   * Used for auditing.
   */
  String INITIATOR = "initiator";

  /**
   * Define an output variable that can be retrieved later with ${activityId.outputs.name}.
   */
  void setOutputVariable(String name, Object value);

  /**
   * @return Gateway to access the BDK services.
   */
  BdkGateway bdk();

  /**
   * @return BDK service to manage connections.
   */
  ConnectionService connections();

  /**
   * @return The activity definition from the workflow.
   */
  T getActivity();

  /**
   * @return Last event captured by the workflow.
   */
  EventHolder<Object> getEvent();

  /**
   * @return Resource file stored with the workflow.
   */
  InputStream getResource(String resourcePath) throws IOException;
}
