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
import com.symphony.bdk.workflow.exception.NotFoundException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

@ExtendWith(MockitoExtension.class)
class WorkflowUpdateActionTest {
  @Mock
  WorkflowDeployer deployer;

  @Mock
  WorkflowEngine<BpmnModelInstance> workflowEngine;

  @InjectMocks
  WorkflowUpdateAction action;

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

  @Test
  void doAction_update_successful() {
    when(deployer.workflowExists(anyString(), eq("v2"))).thenReturn(true);
    when(workflowEngine.parseAndValidate(any(Workflow.class))).thenReturn(mock(BpmnModelInstance.class));
    when(deployer.workflowSwadlPath(anyString(), anyString())).thenReturn(Path.of("./workflows"));
    WorkflowUpdateAction spied = spy(action);
    doNothing().when(spied).writeFile(anyString(), any(Workflow.class), anyString());
    spied.doAction(swadl);
    verify(spied).writeFile(anyString(), any(Workflow.class), anyString());
  }

  @Test
  void doAction_invalidSwadl_exception() {
    String swadl = "activities:\n"
        + "  - send-message:\n"
        + "      id: msg\n"
        + "      on:\n"
        + "        message-received:\n"
        + "          content: msg\n"
        + "      content: content";
    assertThatThrownBy(() -> action.doAction(swadl), "Invalid swadl must fail").isInstanceOf(
        IllegalArgumentException.class).hasMessage("SWADL content is not valid");
  }

  @Test
  void doAction_updateNonExisting_exception() {
    when(deployer.workflowExists(anyString(), eq("v2"))).thenReturn(false);
    assertThatThrownBy(() -> action.doAction(swadl), "No existing swadl must fail").isInstanceOf(
        NotFoundException.class).hasMessage("Version v2 of the workflow test does not exist");
  }

  @Test
  void action() {
    assertThat(action.action()).isEqualTo(WorkflowMgtAction.UPDATE);
  }
}
