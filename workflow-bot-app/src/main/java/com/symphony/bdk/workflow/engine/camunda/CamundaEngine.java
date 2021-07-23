package com.symphony.bdk.workflow.engine.camunda;

import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.engine.camunda.bpmn.CamundaBpmnBuilder;
import com.symphony.bdk.workflow.lang.swadl.Workflow;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class CamundaEngine implements WorkflowEngine {

  @Autowired
  private RepositoryService repositoryService;

  @Autowired
  private CamundaBpmnBuilder bpmnBuilder;

  @Autowired
  private EventToMessage eventToMessage;

  @Override
  public void execute(Workflow workflow) throws IOException {
    bpmnBuilder.addWorkflow(workflow);
    log.info("Deployed workflow {}", workflow.getName());
  }

  @Override
  public void stop(String workflowName) {
    for (Deployment deployment : repositoryService.createDeploymentQuery().deploymentName(workflowName).list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
      log.info("Removed workflow {}", deployment.getName());
    }
  }

  @Override
  public void stopAll() {
    for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
      log.info("Removed workflow {}", deployment.getName());
    }
  }

  @SneakyThrows
  @Override
  public <T> Optional<String> onEvent(RealTimeEvent<T> event) {
    MessageCorrelationBuilder messageCorrelation = eventToMessage.toMessage(event);

    List<MessageCorrelationResult> messageCorrelationResult = messageCorrelation.correlateAllWithResult();

    log.info("Event {} resulted in {}", event, messageCorrelationResult);
    return messageCorrelationResult.stream()
        .findFirst()
        .map(MessageCorrelationResult::getProcessInstance)
        .map(Execution::getId);
  }

}
