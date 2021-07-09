package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.engine.camunda.bpmn.CamundaBpmnBuilder;
import com.symphony.bdk.workflow.lang.swadl.Workflow;
import com.symphony.bdk.workflow.lang.validator.YamlValidator;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelException;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.xml.ModelValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
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
    BpmnModelInstance instance = bpmnBuilder.addWorkflow(workflow);
    this.generateBpmnOutputFile(instance, workflow);
  }

  @Override
  public void stopAll() {
    for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  private void generateBpmnOutputFile(BpmnModelInstance instance, Workflow workflow) throws IOException {
    String outputBpmnFilename = String.format("./%s.bpmn", workflow.getName());
    File file = new File(outputBpmnFilename);
    if (file.exists()) {
      LOGGER.info("Output bpmn file {} already exists. It will be overridden.", outputBpmnFilename);
    } else {
      boolean successfullyCreated = file.createNewFile();
      String logMessage = successfullyCreated
          ? String.format("Output bpmn file %s is created.", outputBpmnFilename)
          : String.format("Output bpmn file %s is NOT created.", outputBpmnFilename);
      LOGGER.info(logMessage);
    }

    try {
      Bpmn.writeModelToFile(file, instance);
      LOGGER.info("Output bpmn file {} is updated.", outputBpmnFilename);
    } catch (BpmnModelException | ModelValidationException e) {
      LOGGER.error(e.getMessage());
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

    // how do we correlate a sub process
    runtimeService.createMessageCorrelation(FORM_REPLY_PREFIX + formId)
        .processInstanceVariableEquals(formId + ".msgId", messageId)
        .setVariables(Collections.singletonMap(formId, formReplies))
        .correlate();
  }

}
