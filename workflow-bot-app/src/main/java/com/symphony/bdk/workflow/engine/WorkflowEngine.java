package com.symphony.bdk.workflow.engine;

import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import java.io.IOException;

public interface WorkflowEngine {

  void deploy(Workflow workflow, String defaultWorkflowId) throws IOException;

  void execute(String workflowId, ExecutionParameters parameters)
      throws UnauthorizedException, IllegalArgumentException;

  <T> void onEvent(RealTimeEvent<T> event);

  void undeploy(String workflowName);

  void undeployAll();
}
