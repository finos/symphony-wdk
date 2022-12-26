package com.symphony.bdk.workflow.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowMgtAction;
import com.symphony.bdk.workflow.configuration.WorkflowDeployer;
import com.symphony.bdk.workflow.exception.NotFoundException;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;


@ExtendWith(MockitoExtension.class)
class WorkflowDeleteActionTest {
  @Mock
  WorkflowDeployer deployer;

  @InjectMocks
  WorkflowDeleteAction action;

  @Test
  void doAction_delete_successful() {
    Path path = mock(Path.class);
    File file = mock(File.class);
    when(deployer.workflowExist(eq("id"), null)).thenReturn(true);
    when(deployer.workflowSwadlPath(anyString())).thenReturn(Collections.singletonList(path));
    when(path.toFile()).thenReturn(file);
    when(file.delete()).thenReturn(true);
    action.doAction("id");
    boolean delete = verify(file).delete();
    assertThat(delete).isFalse();
  }

  @Test
  void doAction_delete_fileNotExist() {
    Path path = mock(Path.class);
    File file = mock(File.class);
    when(deployer.workflowExist(eq("id"), null)).thenReturn(true);
    when(deployer.workflowSwadlPath(anyString())).thenReturn(Collections.singletonList(path));
    when(path.toFile()).thenReturn(file);
    when(file.delete()).thenReturn(false);
    Assertions.assertThatThrownBy(() -> action.doAction("id"), "Deletion on non-existing file must fail")
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Workflow id does not exist");
  }

  @Test
  void doAction_delete_fileNull() {
    when(deployer.workflowExist(eq("id"), null)).thenReturn(false);
    Assertions.assertThatThrownBy(() -> action.doAction("id"), "Deletion on non-existing file must fail")
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Workflow id does not exist");
  }

  @Test
  void action() {
    assertThat(action.action()).isEqualTo(WorkflowMgtAction.DELETE);
  }
}
