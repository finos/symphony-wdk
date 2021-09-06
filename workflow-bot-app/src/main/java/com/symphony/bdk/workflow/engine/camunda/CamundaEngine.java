package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.engine.camunda.bpmn.CamundaBpmnBuilder;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class CamundaEngine implements WorkflowEngine {

  @Autowired
  private RepositoryService repositoryService;

  @Autowired
  private CamundaBpmnBuilder bpmnBuilder;

  @Autowired
  private WorkflowEventToCamundaEvent events;

  @Autowired
  private AuditTrailLogger auditTrailLogger;

  @Override
  public void execute(Workflow workflow) throws IOException {
    if (workflow.getId() == null) {
      workflow.setId("workflow_" + UUID.randomUUID());
    }
    Deployment deployment = bpmnBuilder.addWorkflow(workflow);
    log.info("Deployed workflow {}", deployment.getId());
    auditTrailLogger.deployed(deployment);
  }

  @Override
  public void stop(String workflowName) {
    for (Deployment deployment : repositoryService.createDeploymentQuery().deploymentName(workflowName).list()) {
      stop(deployment);
    }
  }

  private void stop(Deployment deployment) {
    repositoryService.deleteDeployment(deployment.getId(), true);
    log.info("Removed workflow {}", deployment.getName());
    auditTrailLogger.undeployed(deployment);
  }

  @Override
  public void stopAll() {
    for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      CamundaEngine.this.stop(deployment);
    }
  }

  @SneakyThrows
  @Override
  public <T> void onEvent(RealTimeEvent<T> event) {
    events.dispatch(event);
  }

}
