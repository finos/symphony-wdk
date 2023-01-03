package com.symphony.bdk.workflow.management;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowMgtAction;
import com.symphony.bdk.workflow.engine.camunda.CamundaEngine;
import com.symphony.bdk.workflow.exception.NotFoundException;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;
import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;
import com.symphony.bdk.workflow.versioning.service.VersioningService;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class WorkflowManagementService {

  private final VersioningService versioningService;

  private final WorkflowsMgtActionHolder mgtActionHolder;

  private final CamundaEngine camundaEngine;

  public WorkflowManagementService(VersioningService versioningService, WorkflowsMgtActionHolder mgtActionHolder,
      CamundaEngine camundaEngine) {
    this.versioningService = versioningService;
    this.mgtActionHolder = mgtActionHolder;
    this.camundaEngine = camundaEngine;
  }

  public void deploy(String content) {
    mgtActionHolder.getInstance(WorkflowMgtAction.DEPLOY).doAction(content);
  }

  public void update(String content) {
    mgtActionHolder.getInstance(WorkflowMgtAction.UPDATE).doAction(content);
  }

  public void delete(String id) {
    mgtActionHolder.getInstance(WorkflowMgtAction.DELETE).doAction(id);
  }

  public void setActiveVersion(String workflowId, String version) {
    Optional<VersionedWorkflow> deployedWorkflowOptional =
        this.versioningService.findByWorkflowIdAndVersion(workflowId, version);

    if (deployedWorkflowOptional.isEmpty()) {
      throw new NotFoundException(String.format("Version %s of the workflow %s does not exist", version, workflowId));
    }

    VersionedWorkflow deployedWorkflow = deployedWorkflowOptional.get();
    try {
      Workflow workflowToDeploy = SwadlParser.fromYaml(deployedWorkflow.getSwadl());
      String deploymentId = this.camundaEngine.deploy(workflowToDeploy);
      versioningService.save(deployedWorkflow, deploymentId);

    } catch (IOException | ProcessingException e) {
      throw new IllegalArgumentException("SWADL content is not valid");
    }
  }
}
