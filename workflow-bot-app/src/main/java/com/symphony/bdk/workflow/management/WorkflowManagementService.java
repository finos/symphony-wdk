package com.symphony.bdk.workflow.management;

import com.symphony.bdk.workflow.api.v1.dto.SwadlView;
import com.symphony.bdk.workflow.api.v1.dto.VersionedWorkflowView;
import com.symphony.bdk.workflow.configuration.ConditionalOnPropertyNotEmpty;
import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.engine.camunda.CamundaTranslatedWorkflowContext;
import com.symphony.bdk.workflow.exception.NotFoundException;
import com.symphony.bdk.workflow.management.converter.VersionedWorkflowBiConverter;
import com.symphony.bdk.workflow.management.repository.VersionedWorkflowRepository;
import com.symphony.bdk.workflow.management.repository.domain.VersionedWorkflow;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@ConditionalOnPropertyNotEmpty("wdk.properties.management-token")
@Transactional(propagation = Propagation.REQUIRES_NEW)
@Slf4j
public class WorkflowManagementService {
  private static final String WORKFLOW_NOT_EXIST_EXCEPTION_MSG = "Workflow %s does not exist.";
  private static final String WORKFLOW_UPDATE_FORBIDDEN_EXCEPTION_MSG = "Update on a published Workflow is forbidden.";
  private final WorkflowEngine<CamundaTranslatedWorkflowContext> workflowEngine;
  private final VersionedWorkflowRepository versionRepository;
  private final ObjectConverter objectConverter;

  public void deploy(SwadlView swadlView) {
    Workflow workflow = objectConverter.convert(swadlView.getSwadl(), Workflow.class);
    throwExceptionIfUnPublishedVersionExists(workflow);
    CamundaTranslatedWorkflowContext context = workflowEngine.translate(workflow);
    String deploy = null;
    if (workflow.isToPublish()) {
      deploy = workflowEngine.deploy(context);
      setCurrentActiveVersionToInactive(workflow.getId());
    }
    VersionedWorkflow versionedWorkflow = new VersionedWorkflowBiConverter(deploy).apply(workflow, swadlView);
    versionRepository.save(versionedWorkflow);
  }

  private void throwExceptionIfUnPublishedVersionExists(Workflow workflow) {
    Optional<VersionedWorkflow> notPublished = versionRepository.findByWorkflowIdAndPublishedFalse(workflow.getId());
    notPublished.ifPresent(wf -> {
      throw new IllegalArgumentException(
          String.format("Version %s of workflow has not been published yet.", wf.getVersion()));
    });
  }

  public void update(SwadlView swadlView) {
    Workflow workflow = objectConverter.convert(swadlView.getSwadl(), Workflow.class);

    VersionedWorkflow versionedWorkflow = readAndCheckIfLatestNonPublished(workflow);
    versionedWorkflow.setVersion(workflow.getVersion());
    versionedWorkflow.setSwadl(swadlView.getSwadl());
    versionedWorkflow.setDescription(swadlView.getDescription());
    versionedWorkflow.setPublished(workflow.isToPublish());

    CamundaTranslatedWorkflowContext context = workflowEngine.translate(workflow);
    if (workflow.isToPublish()) {
      String deploy = workflowEngine.deploy(context);
      setCurrentActiveVersionToInactive(workflow.getId());
      versionedWorkflow.setDeploymentId(deploy);
      versionedWorkflow.setActive(true);
    }
    versionRepository.save(versionedWorkflow);
  }

  private VersionedWorkflow readAndCheckIfLatestNonPublished(Workflow workflow) {
    Optional<VersionedWorkflow> latestVersion =
        versionRepository.findTopByWorkflowIdOrderByVersionDesc(workflow.getId());
    if (latestVersion.isEmpty()) {
      throw new NotFoundException(String.format(WORKFLOW_NOT_EXIST_EXCEPTION_MSG, workflow.getId()));
    }

    if (latestVersion.get().getPublished()) {
      throw new UnsupportedOperationException(WORKFLOW_UPDATE_FORBIDDEN_EXCEPTION_MSG);
    }

    // The list can only contain 1 draft version item
    return latestVersion.get();
  }

  private void setCurrentActiveVersionToInactive(String workflow) {
    Optional<VersionedWorkflow> activeVersion = versionRepository.findByWorkflowIdAndActiveTrue(workflow);
    activeVersion.ifPresent(activeWorkflow -> {
      activeWorkflow.setActive(null);
      versionRepository.saveAndFlush(activeWorkflow);
    });
  }

  public Optional<VersionedWorkflowView> get(String id) {
    Optional<VersionedWorkflow> activeVersion = versionRepository.findByWorkflowIdAndActiveTrue(id);
    return activeVersion.map(w -> objectConverter.convert(w, VersionedWorkflowView.class));
  }

  public Optional<VersionedWorkflowView> get(String id, Long version) {
    Optional<VersionedWorkflow> versionedWorkflow = versionRepository.findByWorkflowIdAndVersion(id, version);
    return versionedWorkflow.map(w -> objectConverter.convert(w, VersionedWorkflowView.class));
  }

  public List<VersionedWorkflowView> getAllVersions(String id) {
    List<VersionedWorkflow> workflows = versionRepository.findByWorkflowId(id);
    return objectConverter.convertCollection(workflows, VersionedWorkflowView.class);
  }

  public void delete(String id) {
    this.delete(id, null);
  }

  public void delete(String id, Long version) {
    Optional.ofNullable(version).ifPresentOrElse(ver -> {
      Optional<VersionedWorkflow> workflow = versionRepository.findByWorkflowIdAndVersion(id, ver);
      workflow.ifPresent(w -> {
        versionRepository.deleteByWorkflowIdAndVersion(id, ver);
        if (w.getActive()) {
          workflowEngine.undeployByDeploymentId(w.getDeploymentId());
        }
      });
    }, () -> {
      versionRepository.deleteByWorkflowId(id);
      workflowEngine.undeployByWorkflowId(id);
    });
  }

  public void setActiveVersion(String workflowId, Long version) {
    VersionedWorkflow deployedWorkflow = validateWorkflowVersion(workflowId, version);
    Workflow workflowToDeploy = objectConverter.convert(deployedWorkflow.getSwadl(), version, Workflow.class);
    String deploymentId = workflowEngine.deploy(workflowToDeploy);
    setCurrentActiveVersionToInactive(workflowId);
    deployedWorkflow.setDeploymentId(deploymentId);
    deployedWorkflow.setActive(true);
    versionRepository.save(deployedWorkflow);
  }

  private VersionedWorkflow validateWorkflowVersion(String workflowId, Long version) {
    Optional<VersionedWorkflow> optionalDeployed = versionRepository.findByWorkflowIdAndVersion(workflowId, version);
    if (optionalDeployed.isEmpty()) {
      throw new NotFoundException(String.format("Version %s of the workflow %s does not exist.", version, workflowId));
    }

    VersionedWorkflow deployedWorkflow = optionalDeployed.get();
    if (!deployedWorkflow.getPublished()) {
      throw new IllegalArgumentException(
          String.format("Version %s of the workflow %s is in draft mode.", version, workflowId));
    }
    return deployedWorkflow;
  }
}
