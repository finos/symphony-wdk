package com.symphony.bdk.workflow.management;

import com.symphony.bdk.workflow.api.v1.dto.SwadlView;
import com.symphony.bdk.workflow.api.v1.dto.VersionedWorkflowView;
import com.symphony.bdk.workflow.configuration.ConditionalOnPropertyNotEmpty;
import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.engine.camunda.CamundaTranslatedWorkflowContext;
import com.symphony.bdk.workflow.exception.NotFoundException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;
import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;
import com.symphony.bdk.workflow.versioning.repository.VersionedWorkflowRepository;

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
  private final WorkflowEngine<CamundaTranslatedWorkflowContext> workflowEngine;
  private final VersionedWorkflowRepository versionRepository;
  private final ObjectConverter objectConverter;

  public void deploy(SwadlView swadlView) {
    Workflow workflow = objectConverter.convert(swadlView.getSwadl(), Workflow.class);
    CamundaTranslatedWorkflowContext context = workflowEngine.translate(workflow);

    versionRepository.findByWorkflowIdAndPublishedFalse(workflow.getId())
        .ifPresent(wf -> {
          throw new IllegalArgumentException(
              String.format("Version %s of workflow has not been published yet.", wf.getVersion()));
        });

    if (workflow.isToPublish()) {
      publishWorkflow(swadlView, context);
    } else {
      VersionedWorkflow versionedWorkflow = toVersionedWorkflow(workflow, swadlView, null);
      versionRepository.save(versionedWorkflow);
    }
  }

  private void publishWorkflow(SwadlView swadlView, CamundaTranslatedWorkflowContext context) {
    String deploy = workflowEngine.deploy(context);
    Workflow workflow = context.getWorkflow();
    VersionedWorkflow versionedWorkflow = toVersionedWorkflow(workflow, swadlView, deploy);
    Optional<VersionedWorkflow> activeVersion = versionRepository.findByWorkflowIdAndActiveTrue(workflow.getId());
    if (activeVersion.isPresent()) {
      VersionedWorkflow activeWorkflow = activeVersion.get();
      activeWorkflow.setActive(null);
      versionRepository.saveAndFlush(activeWorkflow);
    }
    versionRepository.save(versionedWorkflow);
  }

  private VersionedWorkflow toVersionedWorkflow(Workflow workflow, SwadlView swadlView, String deploymentId) {
    Optional<String> optionalDeployId = Optional.ofNullable(deploymentId);
    VersionedWorkflow versionedWorkflow = new VersionedWorkflow();
    versionedWorkflow.setWorkflowId(workflow.getId());
    versionedWorkflow.setVersion(workflow.getVersion());
    versionedWorkflow.setDeploymentId(optionalDeployId.orElse(null));
    versionedWorkflow.setSwadl(swadlView.getSwadl());
    versionedWorkflow.setDescription(swadlView.getDescription());
    versionedWorkflow.setUserId(swadlView.getAuthor());
    versionedWorkflow.setPublished(workflow.isToPublish());
    versionedWorkflow.setActive(optionalDeployId.isPresent() ? true : null);
    versionedWorkflow.setDeploymentId(optionalDeployId.orElse(null));
    return versionedWorkflow;
  }

  public void update(SwadlView swadlView) {
    Workflow workflow = objectConverter.convert(swadlView.getSwadl(), Workflow.class);
    VersionedWorkflow versionedWorkflow = readAndValidate(workflow);
    CamundaTranslatedWorkflowContext context = workflowEngine.translate(workflow);
    versionedWorkflow.setVersion(workflow.getVersion());
    versionedWorkflow.setSwadl(swadlView.getSwadl());
    versionedWorkflow.setDescription(swadlView.getDescription());
    versionedWorkflow.setPublished(workflow.isToPublish());
    if (workflow.isToPublish()) {
      String deploy = workflowEngine.deploy(context);
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

  private VersionedWorkflow readAndValidate(Workflow workflow) {
    Optional<VersionedWorkflow> activeVersion = versionRepository.findByWorkflowIdAndPublishedFalse(workflow.getId());
    if (activeVersion.isEmpty()) {
      throw new NotFoundException(String.format(WORKFLOW_NOT_EXIST_EXCEPTION_MSG, workflow.getId()));
    }
    if (activeVersion.get().getPublished()) {
      throw new IllegalArgumentException("Update on a published Workflow is forbidden.");
    }
    return activeVersion.get();
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
