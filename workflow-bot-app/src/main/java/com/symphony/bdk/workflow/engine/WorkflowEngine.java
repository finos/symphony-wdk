package com.symphony.bdk.workflow.engine;

import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.exception.UnauthorizedException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import java.io.IOException;

public interface WorkflowEngine<K> {

  void deploy(Workflow workflow) throws IOException;

  void deploy(Workflow workflow, Object instance) throws IOException;

  K parseAndValidate(Workflow workflow);

  void execute(String workflowId, ExecutionParameters parameters) throws UnauthorizedException;

  <T> void onEvent(RealTimeEvent<T> event);

  void undeploy(String workflowName);

  void undeployAll();
}
