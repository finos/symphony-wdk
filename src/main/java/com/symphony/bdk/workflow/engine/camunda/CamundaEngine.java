package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.engine.camunda.bpmn.CamundaBpmnBuilder;
import com.symphony.bdk.workflow.lang.swadl.Workflow;
import com.symphony.bdk.workflow.lang.validator.YamlValidator;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Component
public class CamundaEngine implements WorkflowEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaEngine.class);
  private static final String STREAM_ID = "streamId";
  private static final String MESSAGE_PREFIX = "message_";
  private static final String FORM_REPLY_PREFIX = "formReply_";

  @Autowired
  private RuntimeService runtimeService;

  @Autowired
  private RepositoryService repositoryService;

  @Autowired
  private CamundaBpmnBuilder bpmnBuilder;

  @Override
  public void execute(Workflow workflow) throws IOException {
    bpmnBuilder.addWorkflow(workflow);
  }

  @Override
  public void stopAll() {
    for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Override
  public void messageReceived(String streamId, String content) {
    if (!content.startsWith(YamlValidator.YAML_VALIDATION_COMMAND)) {
      // content being the command to start a workflow
      runtimeService.startProcessInstanceByMessage(MESSAGE_PREFIX + content,
          Collections.singletonMap(STREAM_ID, streamId));
    }
  }

  @Override
  public void formReceived(String messageId, String formId, Map<String, Object> formReplies) {
    // we expect the activity id to be the same as the form id to work
    // correlation across processes is based on the message id tha was created to send the form
    runtimeService.createMessageCorrelation(FORM_REPLY_PREFIX + formId)
        .processInstanceVariableEquals(formId + ".msgId", messageId)
        .setVariables(Collections.singletonMap(formId, formReplies))
        .correlate();
  }

}
