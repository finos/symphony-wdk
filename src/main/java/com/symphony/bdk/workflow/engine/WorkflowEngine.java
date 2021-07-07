package com.symphony.bdk.workflow.engine;

import com.symphony.bdk.workflow.context.WorkflowContext;

import java.io.IOException;
import java.util.Map;

public interface WorkflowEngine {

  String execute(WorkflowContext workflowContext) throws IOException;

  void messageReceived(String streamId, String content);

  void formReceived(String formId, String name, Map<String, Object> formReplies);
}
