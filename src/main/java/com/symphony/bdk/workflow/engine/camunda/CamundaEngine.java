package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.engine.camunda.bpmn.CamundaBpmnBuilder;
import com.symphony.bdk.workflow.lang.swadl.Workflow;
import com.symphony.bdk.workflow.lang.validator.YamlValidator;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
public class CamundaEngine implements WorkflowEngine {

  private static final String STREAM_ID = "streamId";
  private static final String MESSAGE_PREFIX = "message_";

  @Autowired
  private RuntimeService runtimeService;

  @Autowired
  private CamundaBpmnBuilder bpmnBuilder;

  @Override
  public void execute(Workflow workflow) {
    bpmnBuilder.addWorkflow(workflow);
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

}
