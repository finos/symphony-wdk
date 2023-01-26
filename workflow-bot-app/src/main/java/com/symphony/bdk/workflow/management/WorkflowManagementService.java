package com.symphony.bdk.workflow.management;

import com.symphony.bdk.workflow.configuration.ConditionalOnPropertyNotEmpty;
import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.exception.NotFoundException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;
import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;
import com.symphony.bdk.workflow.versioning.repository.VersionedWorkflowRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@ConditionalOnPropertyNotEmpty("wdk.properties.management-token")
@Transactional(propagation = Propagation.REQUIRES_NEW)
@Slf4j
public class WorkflowManagementService {
  private static final String WORKFLOW_NOT_EXIST_EXCEPTION_MSG = "Workflow %s does not exist.";
  private final WorkflowEngine<BpmnModelInstance> workflowEngine;
  private final VersionedWorkflowRepository versioningRepository;
  private final ObjectConverter objectConverter;

  public void deploy(String content) {
    Workflow workflow = objectConverter.convert(content, Workflow.class);
    BpmnModelInstance modelInstance = workflowEngine.parseAndValidate(workflow);
    if (workflow.isToPublish()) {
      publishWorkflow(content, workflow, modelInstance);
    } else {
      VersionedWorkflow versionedWorkflow = toVersionedWorkflow(workflow, content, Optional.empty());
      versioningRepository.save(versionedWorkflow);
    }
  }

  private void publishWorkflow(String content, Workflow workflow, BpmnModelInstance modelInstance) {
    String deploy = workflowEngine.deploy(workflow, modelInstance);
    VersionedWorkflow versionedWorkflow = toVersionedWorkflow(workflow, content, Optional.of(deploy));
    Optional<VersionedWorkflow> activeVersion =
        versioningRepository.findByWorkflowIdAndActiveTrue(workflow.getId());
    if (activeVersion.isPresent()) {
      VersionedWorkflow activeWorkflow = activeVersion.get();
      activeWorkflow.setActive(null);
      versioningRepository.saveAndFlush(activeWorkflow);
    }
    versioningRepository.save(versionedWorkflow);
  }

  private VersionedWorkflow toVersionedWorkflow(Workflow workflow, String content, Optional<String> deploymentId) {
    VersionedWorkflow versionedWorkflow = new VersionedWorkflow();
    versionedWorkflow.setWorkflowId(workflow.getId());
    versionedWorkflow.setVersion(ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now()));
    versionedWorkflow.setDeploymentId(deploymentId.orElse(null));
    versionedWorkflow.setSwadl(content);
    versionedWorkflow.setPublished(workflow.isToPublish());
    versionedWorkflow.setActive(deploymentId.isPresent() ? true : null);
    versionedWorkflow.setDeploymentId(deploymentId.orElse(null));
    return versionedWorkflow;
  }

  public void update(String content) {
    Workflow workflow = objectConverter.convert(content, Workflow.class);
    Optional<VersionedWorkflow> activeVersion =
        versioningRepository.findFirstByWorkflowIdOrderByVersionDesc(workflow.getId());
    VersionedWorkflow versionedWorkflow = validateUpdateOperation(workflow, activeVersion);
    BpmnModelInstance modelInstance = workflowEngine.parseAndValidate(workflow);
    versionedWorkflow.setVersion(ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now()));
    versionedWorkflow.setSwadl(content);
    versionedWorkflow.setPublished(workflow.isToPublish());
    if (workflow.isToPublish()) {
      String deploy = workflowEngine.deploy(workflow, modelInstance);
      versionedWorkflow.setDeploymentId(deploy);
      versionedWorkflow.setActive(true);
    }
    versioningRepository.save(versionedWorkflow);
  }

  private VersionedWorkflow validateUpdateOperation(Workflow workflow, Optional<VersionedWorkflow> activeVersion) {
    if (activeVersion.isEmpty()) {
      throw new NotFoundException(String.format(WORKFLOW_NOT_EXIST_EXCEPTION_MSG, workflow.getId()));
    }
    VersionedWorkflow versionedWorkflow = activeVersion.get();
    if (versionedWorkflow.getPublished()) {
      throw new IllegalArgumentException("Update on a published Workflow is forbidden.");
    }
    return versionedWorkflow;
  }

  public void delete(String id) {
    boolean exist = versioningRepository.existsByWorkflowId(id);
    if (!exist) {
      throw new NotFoundException(String.format(WORKFLOW_NOT_EXIST_EXCEPTION_MSG, id));
    }
    workflowEngine.undeployByWorkflowId(id);
    versioningRepository.deleteByWorkflowId(id);
  }

  public void setActiveVersion(String workflowId, String version) {
    Optional<VersionedWorkflow> deployedWorkflowOptional =
        versioningRepository.findByWorkflowIdAndVersion(workflowId, Long.valueOf(version));
    if (deployedWorkflowOptional.isEmpty()) {
      throw new NotFoundException(String.format("Version %s of the workflow %s does not exist.", version, workflowId));
    }

    VersionedWorkflow deployedWorkflow = deployedWorkflowOptional.get();
    if (!deployedWorkflow.getPublished()) {
      throw new IllegalArgumentException(
          String.format("Version %s of the workflow %s is in draft mode.", version, workflowId));
    }

    Optional<VersionedWorkflow> activeVersion = versioningRepository.findByWorkflowIdAndActiveTrue(workflowId);
    activeVersion.ifPresent(versionedWorkflow -> {
      versionedWorkflow.setActive(null);
      versioningRepository.saveAndFlush(versionedWorkflow);
    });

    Workflow workflowToDeploy = objectConverter.convert(deployedWorkflow.getSwadl(), Workflow.class);
    String deploymentId = workflowEngine.deploy(workflowToDeploy);
    deployedWorkflow.setDeploymentId(deploymentId);
    deployedWorkflow.setActive(true);
    versioningRepository.save(deployedWorkflow);
  }
}
