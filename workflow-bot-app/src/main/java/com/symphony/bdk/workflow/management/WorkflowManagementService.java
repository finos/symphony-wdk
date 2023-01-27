package com.symphony.bdk.workflow.management;

import com.symphony.bdk.workflow.api.v1.dto.SwadlView;
import com.symphony.bdk.workflow.api.v1.dto.VersionedWorkflowView;
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
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@ConditionalOnPropertyNotEmpty("wdk.properties.management-token")
@Transactional(propagation = Propagation.REQUIRES_NEW)
@Slf4j
public class WorkflowManagementService {
  private static final String WORKFLOW_NOT_EXIST_EXCEPTION_MSG = "Workflow %s does not exist.";
  private final WorkflowEngine<BpmnModelInstance> workflowEngine;
  private final VersionedWorkflowRepository versionRepository;
  private final ObjectConverter objectConverter;

  public void deploy(SwadlView swadlView) {
    Workflow workflow = objectConverter.convert(swadlView.getSwadl(), Workflow.class);
    BpmnModelInstance modelInstance = workflowEngine.parseAndValidate(workflow);
    Optional<VersionedWorkflow> notPublished = versionRepository.findByWorkflowIdAndPublishedFalse(workflow.getId());
    notPublished.ifPresent(wf -> {
      throw new IllegalArgumentException(
          String.format("Version %s of workflow has not published yet.", wf.getVersion()));
    });
    if (workflow.isToPublish()) {
      publishWorkflow(swadlView, workflow, modelInstance);
    } else {
      VersionedWorkflow versionedWorkflow = toVersionedWorkflow(workflow, swadlView, Optional.empty());
      versionRepository.save(versionedWorkflow);
    }
  }

  private void publishWorkflow(SwadlView swadlView, Workflow workflow, BpmnModelInstance modelInstance) {
    String deploy = workflowEngine.deploy(workflow, modelInstance);
    VersionedWorkflow versionedWorkflow = toVersionedWorkflow(workflow, swadlView, Optional.of(deploy));
    Optional<VersionedWorkflow> activeVersion = versionRepository.findByWorkflowIdAndActiveTrue(workflow.getId());
    if (activeVersion.isPresent()) {
      VersionedWorkflow activeWorkflow = activeVersion.get();
      activeWorkflow.setActive(null);
      versionRepository.saveAndFlush(activeWorkflow);
    }
    versionRepository.save(versionedWorkflow);
  }

  private VersionedWorkflow toVersionedWorkflow(Workflow workflow, SwadlView swadlView, Optional<String> deploymentId) {
    VersionedWorkflow versionedWorkflow = new VersionedWorkflow();
    versionedWorkflow.setWorkflowId(workflow.getId());
    versionedWorkflow.setVersion(ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now()));
    versionedWorkflow.setDeploymentId(deploymentId.orElse(null));
    versionedWorkflow.setSwadl(swadlView.getSwadl());
    versionedWorkflow.setDescription(swadlView.getDescription());
    versionedWorkflow.setPublished(workflow.isToPublish());
    versionedWorkflow.setActive(deploymentId.isPresent() ? true : null);
    versionedWorkflow.setDeploymentId(deploymentId.orElse(null));
    return versionedWorkflow;
  }

  public void update(SwadlView swadlView) {
    Workflow workflow = objectConverter.convert(swadlView.getSwadl(), Workflow.class);
    Optional<VersionedWorkflow> unpublished = versionRepository.findByWorkflowIdAndPublishedFalse(workflow.getId());
    VersionedWorkflow versionedWorkflow = validateUpdateOperation(workflow, unpublished);
    BpmnModelInstance modelInstance = workflowEngine.parseAndValidate(workflow);
    versionedWorkflow.setVersion(ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now()));
    versionedWorkflow.setSwadl(swadlView.getSwadl());
    versionedWorkflow.setDescription(swadlView.getDescription());
    versionedWorkflow.setPublished(workflow.isToPublish());
    if (workflow.isToPublish()) {
      String deploy = workflowEngine.deploy(workflow, modelInstance);
      versionedWorkflow.setDeploymentId(deploy);
      versionedWorkflow.setActive(true);
    }
    versionRepository.save(versionedWorkflow);
  }

  public List<VersionedWorkflowView> get(String id) {
    List<VersionedWorkflow> workflows = versionRepository.findByWorkflowId(id);
    return objectConverter.convertCollection(workflows, VersionedWorkflowView.class);
  }

  public Optional<VersionedWorkflowView> get(String id, Long version) {
    Optional<VersionedWorkflow> versionedWorkflow = versionRepository.findByWorkflowIdAndVersion(id, version);
    return versionedWorkflow.map(w -> objectConverter.convert(w, VersionedWorkflowView.class));
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

  public void delete(String id, Optional<Long> version) {
    version.ifPresentOrElse(ver -> {
      Optional<VersionedWorkflow> workflow = versionRepository.findByWorkflowIdAndVersion(id, ver);
      workflow.ifPresent(w -> {
        versionRepository.deleteByWorkflowIdAndVersion(id, ver);
        if (w.getActive()) {
          workflowEngine.undeployByWorkflowId(w.getWorkflowId());
        }
      });
    }, () -> {
      versionRepository.deleteByWorkflowId(id);
      workflowEngine.undeployByWorkflowId(id);
    });
  }

  public void setActiveVersion(String workflowId, Long version) {
    Optional<VersionedWorkflow> optionalDeployed = versionRepository.findByWorkflowIdAndVersion(workflowId, version);
    if (optionalDeployed.isEmpty()) {
      throw new NotFoundException(String.format("Version %s of the workflow %s does not exist.", version, workflowId));
    }

    VersionedWorkflow deployedWorkflow = optionalDeployed.get();
    if (!deployedWorkflow.getPublished()) {
      throw new IllegalArgumentException(
          String.format("Version %s of the workflow %s is in draft mode.", version, workflowId));
    }

    Optional<VersionedWorkflow> activeVersion = versionRepository.findByWorkflowIdAndActiveTrue(workflowId);
    activeVersion.ifPresent(versionedWorkflow -> {
      versionedWorkflow.setActive(null);
      versionRepository.saveAndFlush(versionedWorkflow);
    });

    Workflow workflowToDeploy = objectConverter.convert(deployedWorkflow.getSwadl(), Workflow.class);
    String deploymentId = workflowEngine.deploy(workflowToDeploy);
    deployedWorkflow.setDeploymentId(deploymentId);
    deployedWorkflow.setActive(true);
    versionRepository.save(deployedWorkflow);
  }
}
