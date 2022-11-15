package com.symphony.bdk.workflow.management;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowMgtAction;
import com.symphony.bdk.workflow.configuration.WorkflowDeployer;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.exception.DuplicateException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@Slf4j
@ConditionalOnProperty(value = "wdk.workflows.path")
public class WorkflowDeployAction extends WorkflowAbstractAction implements WorkflowsMgtAction {
  private final WorkflowEngine<BpmnModelInstance> workflowEngine;
  private final String workflowFolder;

  public WorkflowDeployAction(WorkflowEngine<BpmnModelInstance> workflowEngine, WorkflowDeployer deployer,
      @Value("wdk.workflows.path") String workflowFolder) {
    super(deployer);
    this.workflowEngine = workflowEngine;
    this.workflowFolder = workflowFolder;
  }

  @Override
  public void doAction(String content) {
    Workflow workflow = this.convertToWorkflow(content);
    String path = workflowFolder + File.separator + workflow.getId() + ".swadl.yaml";
    log.debug("New workflow path is [{}]", path);
    validateWorkflowUniqueness(workflow);
    validateFilePath(path);
    workflowEngine.parseAndValidate(workflow);
    writeFile(content, workflow, path);
  }

  private void validateWorkflowUniqueness(Workflow workflow) {
    if (this.workflowExist(workflow.getId())) {
      throw new DuplicateException(String.format("Workflow %s already exists", workflow.getId()));
    }
  }

  @Override
  public WorkflowMgtAction action() {
    return WorkflowMgtAction.DEPLOY;
  }
}
