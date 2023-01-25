package com.symphony.bdk.workflow.expiration;

import com.symphony.bdk.workflow.api.v1.dto.DeploymentExpirationEnum;
import com.symphony.bdk.workflow.configuration.ConditionalOnPropertyNotEmpty;
import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.exception.NotFoundException;
import com.symphony.bdk.workflow.management.WorkflowManagementService;
import com.symphony.bdk.workflow.scheduled.RunnableScheduledJob;
import com.symphony.bdk.workflow.scheduled.ScheduledJobsRegistry;
import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;
import com.symphony.bdk.workflow.versioning.model.WorkflowExpirationJob;
import com.symphony.bdk.workflow.versioning.repository.VersionedWorkflowRepository;
import com.symphony.bdk.workflow.versioning.repository.WorkflowExpirationJobRepository;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@ConditionalOnPropertyNotEmpty("wdk.properties.management-token")
@Transactional(propagation = Propagation.REQUIRES_NEW)
@Slf4j
public class WorkflowExpirationService extends WorkflowManagementService implements WorkflowExpirationInterface {
  private final WorkflowExpirationJobRepository expirationJobRepository;
  private final RepositoryService repositoryService;
  private final ScheduledJobsRegistry scheduledJobsRegistry;

  public WorkflowExpirationService(WorkflowExpirationJobRepository workflowExpirationJobRepository,
      RepositoryService repositoryService,
      ScheduledJobsRegistry scheduledJobsRegistry, WorkflowEngine<BpmnModelInstance> workflowEngine,
      VersionedWorkflowRepository versionedWorkflowRepository, ObjectConverter objectConverter) {
    super(workflowEngine, versionedWorkflowRepository, objectConverter);
    this.expirationJobRepository = workflowExpirationJobRepository;
    this.repositoryService = repositoryService;
    this.scheduledJobsRegistry = scheduledJobsRegistry;
  }

  @Override
  public void scheduleWorkflowExpiration(String workflowId, String type, Instant instant) {
    Map<String, String> idToDeploymentMap = new HashMap<>();

    DeploymentExpirationEnum deploymentExpirationEnum = DeploymentExpirationEnum.toDeploymentExpirationEnum(type);
    if (DeploymentExpirationEnum.ACTIVE.equals(deploymentExpirationEnum)) {
      versioningRepository.findByWorkflowIdAndActiveTrue(workflowId)
          .ifPresent(versionedWorkflow -> idToDeploymentMap.put(versionedWorkflow.getId(),
              versionedWorkflow.getDeploymentId()));
    } else {
      idToDeploymentMap.putAll(versioningRepository.findByWorkflowId(workflowId)
          .stream()
          .collect(Collectors.toMap(VersionedWorkflow::getId, VersionedWorkflow::getDeploymentId)));
    }

    if (idToDeploymentMap.isEmpty() && DeploymentExpirationEnum.ACTIVE.equals(deploymentExpirationEnum)) {
      throw new NotFoundException(String.format("No active deployment is found for workflow %s", workflowId));
    } else if (idToDeploymentMap.isEmpty()) {
      throw new NotFoundException(String.format(WORKFLOW_NOT_EXIST_EXCEPTION_MSG, workflowId));
    }

    expirationJobRepository.saveAll(idToDeploymentMap.keySet()
        .stream()
        .map(id -> new WorkflowExpirationJob(id, workflowId, idToDeploymentMap.get(id), deploymentExpirationEnum.name(),
            instant))
        .collect(Collectors.toList()));

    idToDeploymentMap.forEach((id, deploymentId) -> {
      extracted(id, deploymentId, workflowId, instant, deploymentExpirationEnum);
    });
    //extracted(workflowId, instant, idToDeploymentMap, deploymentExpirationEnum);
  }

  @Override
  public void extracted(String id, String deploymentId, String workflowId,
      Instant instant, DeploymentExpirationEnum deploymentExpirationEnum) {
    scheduledJobsRegistry.scheduleJob(new RunnableScheduledJob(() -> {
      if (DeploymentExpirationEnum.ACTIVE.equals(deploymentExpirationEnum)) {
        return String.format("%s.ACTIVE.%s", workflowId, instant);
      } else {
        return String.format("%s.ALL.%s", workflowId, instant);
      }
    }, Duration.between(Instant.now(), instant).getSeconds(),
        () -> {
          versioningRepository.deleteById(id);
          expirationJobRepository.deleteById(id);
          workflowEngine.undeployByDeploymentId(deploymentId);

          Optional<ProcessDefinition> activeProcessDefinition = repositoryService.createProcessDefinitionQuery()
              .processDefinitionKey(workflowId)
              .active()
              .latestVersion()
              .list()
              .stream()
              .findFirst();

          if (activeProcessDefinition.isPresent()) {
            String activeDeploymentId = activeProcessDefinition.get().getDeploymentId();
            Optional<VersionedWorkflow> versionedWorkflow = versioningRepository.findByWorkflowId(workflowId)
                .stream()
                .filter(workflow -> workflow.getDeploymentId().equals(activeDeploymentId))
                .findFirst();

            versionedWorkflow.ifPresent(workflow -> this.setActiveVersionWithoutDeployment(workflow.getWorkflowId(),
                String.valueOf(workflow.getVersion())));
          }
        }));
  }
/*
  public void extracted(String workflowId, Instant instant, Map<String, String> idToDeploymentMap,
      DeploymentExpirationEnum deploymentExpirationEnum) {
    scheduledJobsRegistry.scheduleJob(new RunnableScheduledJob(() -> {
      if (DeploymentExpirationEnum.ACTIVE.equals(deploymentExpirationEnum)) {
        return String.format("%s.ACTIVE.%s", workflowId, instant);
      } else {
        return String.format("%s.ALL.%s", workflowId, instant);
      }
    }, Duration.between(Instant.now(), instant).getSeconds(),
        () -> idToDeploymentMap.forEach((versionedWorkflowId, deploymentId) -> {
          versioningRepository.deleteById(versionedWorkflowId);
          expirationJobRepository.deleteById(versionedWorkflowId);
          workflowEngine.undeployByDeploymentId(deploymentId);

          Optional<ProcessDefinition> activeProcessDefinition = repositoryService.createProcessDefinitionQuery()
              .processDefinitionKey(workflowId)
              .active()
              .latestVersion()
              .list()
              .stream()
              .findFirst();

          if (activeProcessDefinition.isPresent()) {
            String activeDeploymentId = activeProcessDefinition.get().getDeploymentId();
            Optional<VersionedWorkflow> versionedWorkflow = versioningRepository.findByWorkflowId(workflowId)
                .stream()
                .filter(workflow -> workflow.getDeploymentId().equals(activeDeploymentId))
                .findFirst();

            versionedWorkflow.ifPresent(workflow -> this.setActiveVersionWithoutDeployment(workflow.getWorkflowId(),
                String.valueOf(workflow.getVersion())));
          }
        })));
  }*/
}
