package com.symphony.bdk.workflow.management;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowMgtAction;
import com.symphony.bdk.workflow.configuration.WorkflowDeployer;

import org.springframework.stereotype.Service;

@Service
public class WorkflowDeleteAction extends WorkflowAbstractAction implements WorkflowsMgtAction {

  public WorkflowDeleteAction(WorkflowDeployer deployer) {
    super(deployer);
  }

  @Override
  public void doAction(String id) {
    deleteFile(id);
  }

  @Override
  public WorkflowMgtAction action() {
    return WorkflowMgtAction.DELETE;
  }
}
