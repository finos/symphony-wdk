package com.symphony.bdk.workflow.engine;

import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.exception.UnauthorizedException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

public interface WorkflowEngine<K extends TranslatedWorkflowContext> {

  String deploy(Workflow workflow);

  String deploy(K workflowContext);

  K translate(Workflow workflow);

  void execute(String workflowId, ExecutionParameters parameters) throws UnauthorizedException;

  <T> void onEvent(RealTimeEvent<T> event);

  void undeployByWorkflowId(String workflowName);

  void undeployByDeploymentId(String deploymentId);

  void undeployAll();
}
