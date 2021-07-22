package com.symphony.bdk.workflow.engine;

import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.lang.swadl.Workflow;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public interface WorkflowEngine {

  void execute(Workflow workflow) throws IOException;

  Optional<String> messageReceived(String streamId, String content);

  <T> Optional<String> onEvent(RealTimeEvent<T> event);

  /**
   * @param messageId The form's message id (created when the form is submitted)
   */
  void formReceived(String messageId, String formId, Map<String, Object> formReplies);

  void stop(String workflowName);

  void stopAll();
}
