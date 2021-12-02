package com.symphony.bdk.workflow.engine.executor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

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
   * Define output variables that can be retrieved later with ${activityId.outputs.name}.
   */
  void setOutputVariables(Map<String, Object> variables);

  /**
   * Define one output variable that can be retrieved later with ${activityId.outputs.name}.
   */
  void setOutputVariable(String name, Object variable);


  Map<String, Object> getVariables();

  /**
   * @return Gateway to access the BDK services.
   */
  BdkGateway bdk();

  /**
   * @return The activity definition from the workflow.
   */
  T getActivity();

  /**
   * @return Last event captured by the workflow.
   */
  EventHolder<Object> getEvent();

  /**
   * @return Current execution process instance id.
   */
  String getProcessInstanceId();

  /**
   * @return Currently executed activity id.
   */
  String getCurrentActivityId();

  /**
   * @return Resource file stored with the workflow.
   */
  InputStream getResource(Path resourcePath) throws IOException;

  File getResourceFile(Path resourcePath) throws IOException;

  Path saveResource(Path resourcePath, byte[] content) throws IOException;
}
