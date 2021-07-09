package com.symphony.bdk.workflow.engine;

import com.symphony.bdk.workflow.lang.swadl.Workflow;

import java.io.IOException;
import java.util.Map;

public interface WorkflowEngine {

  void execute(Workflow workflow) throws IOException;

  void messageReceived(String streamId, String content);

  void formReceived(String messageId, String formId, Map<String, Object> formReplies);

  void stopAll();
}
