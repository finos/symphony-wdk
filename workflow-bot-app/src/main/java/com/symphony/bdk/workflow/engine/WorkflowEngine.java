package com.symphony.bdk.workflow.engine;

import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import java.io.IOException;
import java.util.Optional;

public interface WorkflowEngine {

  void execute(Workflow workflow) throws IOException;

  <T> Optional<String> onEvent(RealTimeEvent<T> event);

  void stop(String workflowName);

  void stopAll();
}
