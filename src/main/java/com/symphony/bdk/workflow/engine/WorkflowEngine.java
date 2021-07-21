package com.symphony.bdk.workflow.engine;

import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.lang.swadl.Workflow;

import java.io.IOException;
import java.util.Map;

public interface WorkflowEngine {

  void execute(Workflow workflow) throws IOException;

  void messageReceived(String streamId, String content);

  <T> void onEvent(RealTimeEvent<T> event);

  /**
   * @param messageId The form's message id (created when the form is submitted)
   */
  void formReceived(String messageId, String formId, Map<String, Object> formReplies);

  void stopAll();
}
