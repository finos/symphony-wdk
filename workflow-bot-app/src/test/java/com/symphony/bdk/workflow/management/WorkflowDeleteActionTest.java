package com.symphony.bdk.workflow.management;

import com.symphony.bdk.workflow.api.v1.dto.StatusEnum;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowMgtAction;
import com.symphony.bdk.workflow.configuration.WorkflowDeployer;
import com.symphony.bdk.workflow.exception.NotFoundException;
import com.symphony.bdk.workflow.exception.UnprocessableEntityException;
import com.symphony.bdk.workflow.monitoring.service.MonitoringService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class WorkflowDeleteActionTest {
  @Mock
  WorkflowDeployer deployer;

  @Mock
  MonitoringService monitoringService;

  @InjectMocks
  WorkflowDeleteAction action;

  @Test
  void doAction_delete_successful() {
    Path path = mock(Path.class);
    File file = mock(File.class);
    when(deployer.workflowExist(eq("id"))).thenReturn(true);
    when(deployer.workflowSwadlPath(anyString())).thenReturn(Collections.singletonList(path));
    when(monitoringService.listWorkflowInstances(eq("id"), eq(StatusEnum.PENDING.name()))).thenReturn(
            Collections.emptyList());
    when(path.toFile()).thenReturn(file);
    when(file.delete()).thenReturn(true);
    action.doAction("id");
    boolean delete = verify(file).delete();
    assertThat(delete).isFalse();
  }

  @Test
  void doAction_delete_processesArePending() {
    when(monitoringService.listWorkflowInstances(eq("id"), eq(StatusEnum.PENDING.name())))
            .thenReturn(Collections.singletonList(
                    WorkflowInstView.builder().id("id").instanceId("instanceid").status(StatusEnum.PENDING).build()));
    assertThatExceptionOfType(UnprocessableEntityException.class)
            .isThrownBy(() -> action.doAction("id"))
            .satisfies(e -> assertThat(e.getMessage())
                    .isEqualTo("The workflow id cannot be deleted because it has pending processes"));
  }

  @Test
  void doAction_delete_workflowExist_fileNotExist() {
    Path path = mock(Path.class);
    File file = mock(File.class);
    when(deployer.workflowExist(eq("id"))).thenReturn(true);
    when(deployer.workflowSwadlPath(anyString())).thenReturn(Collections.singletonList(path));
    when(monitoringService.listWorkflowInstances(eq("id"), eq(StatusEnum.PENDING.name())))
            .thenReturn(Collections.emptyList());
    when(path.toFile()).thenReturn(file);
    when(file.delete()).thenReturn(false);
    Assertions.assertThatThrownBy(() -> action.doAction("id"), "Deletion on non-existing file must fail")
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Workflow id does not exist");
  }

  @Test
  void doAction_delete_workflowNotExist() {
    when(deployer.workflowExist(eq("id"))).thenReturn(false);
    Assertions.assertThatThrownBy(() -> action.doAction("id"), "Deletion on non-existing file must fail")
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Workflow id does not exist");
  }

  @Test
  void action() {
    assertThat(action.action()).isEqualTo(WorkflowMgtAction.DELETE);
  }
}
