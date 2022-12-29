package com.symphony.bdk.workflow.management;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowMgtAction;
import com.symphony.bdk.workflow.configuration.WorkflowDeployer;
import com.symphony.bdk.workflow.exception.UnprocessableEntityException;
import com.symphony.bdk.workflow.monitoring.service.MonitoringService;

import org.springframework.stereotype.Service;

@Service
public class WorkflowDeleteAction extends WorkflowAbstractAction implements WorkflowsMgtAction {

  public WorkflowDeleteAction(WorkflowDeployer deployer, MonitoringService monitoringService) {
    super(deployer, monitoringService);
  }

  @Override
  public void doAction(String id) {
    if (!canBeDeleted(id)) {
      throw new UnprocessableEntityException(
              String.format("The workflow %s cannot be deleted because it has pending processes", id));
    } else {
      deleteFile(id);
    }
  }

  @Override
  public WorkflowMgtAction action() {
    return WorkflowMgtAction.DELETE;
  }
}
