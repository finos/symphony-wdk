package com.symphony.bdk.workflow.engine.camunda;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import com.symphony.bdk.workflow.context.WorkflowContext;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.engine.camunda.bpmn.CamundaBpmnBuilder;
import com.symphony.bdk.workflow.lang.swadl.Workflow;
import com.symphony.bdk.workflow.lang.validator.YamlValidator;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class CamundaEngine implements WorkflowEngine {

  private final String STREAM_ID = "streamId";
  private final String MESSAGE_PREFIX = "message_";

  @Autowired
  private RuntimeService runtimeService;

  @Autowired
  private CamundaBpmnBuilder bpmnBuilder;

  @Override
  public String execute(WorkflowContext workflowContext) throws IOException {
    // consider a message with an attachment as a workflow to run
    String message = workflowContext.getContentMessage();
    String attachmentId = workflowContext.getAttachmentId();

    if (attachmentId != null && !attachmentId.isEmpty()) {
      if (message.startsWith(YamlValidator.YAML_VALIDATION_COMMAND)) {
        bpmnBuilder.generateBPMNOutputFile(workflowContext.getWorkflow());

        return getSuccessBpmnMessageML(workflowContext.getWorkflow().getName());
      } else {
        bpmnBuilder.addWorkflow(workflowContext.getWorkflow());

        return getSuccessWorkflowMessageML(workflowContext.getWorkflow().getName());
      }
    }

    return getFailMessageML();
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
  public void formReceived(String formId, String name, Map<String, Object> formReplies) {
      runtimeService.createMessageCorrelation(name)
          .processInstanceId(formId)
          .setVariables(formReplies)
          .correlate();
  }

  private Workflow deserializeWorkflow(byte[] workflow) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory()
        .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true));
    return mapper.readValue(workflow, Workflow.class);
  }

  private String getSuccessBpmnMessageML(String workflowName){
    return "<messageML>Ok, validated <b>" + workflowName + "</b></messageML>";
  }

  private String getSuccessWorkflowMessageML(String workflowName){
    return "<messageML>Ok, running workflow <b>" + workflowName + "</b></messageML>";
  }

  private String getFailMessageML(){
    return "<messageML>Failure : Unable to run workflow !</messageML>";
  }
}
