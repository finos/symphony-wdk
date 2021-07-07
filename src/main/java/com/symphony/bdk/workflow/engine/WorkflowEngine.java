package com.symphony.bdk.workflow.engine;

import com.symphony.bdk.workflow.context.WorkflowContext;
import com.symphony.bdk.workflow.lang.swadl.Workflow;

import java.io.IOException;
import java.util.Map;

public interface WorkflowEngine {

  // TODO to be replaced by the one below
  String execute(WorkflowContext workflowContext) throws IOException;

  void execute(Workflow workflow) throws IOException;

  void messageReceived(String streamId, String content);

  void formReceived(String formId, String name, Map<String, Object> formReplies);
}
