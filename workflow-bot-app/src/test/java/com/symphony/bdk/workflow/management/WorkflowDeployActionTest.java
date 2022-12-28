package com.symphony.bdk.workflow.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowMgtAction;
import com.symphony.bdk.workflow.configuration.WorkflowDeployer;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.exception.DuplicateException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WorkflowDeployActionTest {
  @Mock
  WorkflowDeployer deployer;
  @Mock
  WorkflowEngine<BpmnModelInstance> workflowEngine;
  @InjectMocks
  WorkflowDeployAction action;

  private static final String swadl = "id: test\n"
      + "properties:\n"
      + "  version: v2\n"
      + "activities:\n"
      + "  - send-message:\n"
      + "      id: msg\n"
      + "      on:\n"
      + "        message-received:\n"
      + "          content: msg\n"
      + "      content: content";

  @BeforeEach
  public void setUp() {
    ReflectionTestUtils.setField(action, "workflowFolder", "./workflows");
  }

  @Test
  void doAction_deploy_successful() {
    when(deployer.workflowExist(anyString(), eq("v2"))).thenReturn(false);
    when(workflowEngine.parseAndValidate(any(Workflow.class))).thenReturn(mock(BpmnModelInstance.class));
    when(deployer.isPathAlreadyExist(any())).thenReturn(false);
    WorkflowDeployAction spied = spy(action);
    doNothing().when(spied).writeFile(anyString(), any(Workflow.class), anyString());
    spied.doAction(swadl);
    verify(spied).writeFile(anyString(), any(Workflow.class), anyString());
  }

  @Test
  void doAction_deployExistingId_duplicateException() {
    when(deployer.workflowExist(anyString(), eq("v2"))).thenReturn(true);
    assertThatThrownBy(() -> action.doAction(swadl), "Deploy an existing workflow id must fail").isInstanceOf(
        DuplicateException.class).hasMessage("Version v2 of the workflow test already exists");
  }

  @Test
  void doAction_deployExistingSwadl_duplicateException() {
    when(deployer.workflowExist(anyString(), eq("v2"))).thenReturn(false);
    when(deployer.isPathAlreadyExist(any())).thenReturn(true);
    assertThatThrownBy(() -> action.doAction(swadl), "Deploy an existing swadl must fail").isInstanceOf(
        DuplicateException.class).hasMessage("SWADL file already exists");
  }


  @Test
  void action() {
    assertThat(action.action()).isEqualTo(WorkflowMgtAction.DEPLOY);
  }
}
